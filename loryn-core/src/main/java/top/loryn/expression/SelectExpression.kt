package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.LorynDsl
import top.loryn.support.PaginationParams

/**
 * SELECT表达式。
 *
 * @param E 绑定的查询结果的实体类型。
 */
class SelectExpression<E>(
    val columns: List<ColumnExpression<E, *>>,
    val from: QuerySourceExpression<E>?,
    val where: SqlExpression<Boolean>?,
    val groupBy: List<ColumnExpression<E, *>>,
    val having: SqlExpression<Boolean>?,
    val orderBy: List<OrderByExpression<E>>,
    val paginationParams: PaginationParams?,
    val distinct: Boolean,
    private val createEntity: (() -> E)?,
) : EntityCreator<E>, SqlExpression<Nothing> {
    override fun createEntity() =
        if (createEntity != null) {
            createEntity.invoke()
        } else if (from != null) {
            from.createEntity()
        } else {
            super.createEntity()
        }

    private fun SqlBuilder.appendMain(params: MutableList<SqlParam<*>>) = also {
        from?.also {
            append(' ').appendKeyword("FROM").append(' ').appendExpression(it, params)
        }
        where?.also {
            append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params)
        }
        groupBy.takeIf { it.isNotEmpty() }?.also {
            append(' ').appendKeyword("GROUP").append(' ').appendKeyword("BY").append(' ').appendExpressions(it, params)
        }
        having?.also {
            append(' ').appendKeyword("HAVING").append(' ').appendExpression(it, params)
        }
        orderBy.takeIf { it.isNotEmpty() }?.also {
            append(' ').appendKeyword("ORDER").append(' ').appendKeyword("BY").append(' ').appendExpressions(it, params)
        }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("SELECT").append(' ')
        if (distinct) {
            appendKeyword("DISTINCT").append(' ')
        }
        if (columns.isNotEmpty()) {
            appendList(columns, params) { it, params ->
                with(it) { appendSqlInSelectClause(params) }
            }
        } else {
            append('*')
        }
        appendMain(params)
        paginationParams?.also { append(' ').appendPagination(it) }
    }

    fun SqlBuilder.appendSqlCount(column: ColumnExpression<*, *>?, params: MutableList<SqlParam<*>>) = also {
        appendKeyword("SELECT").append(' ').appendKeyword("COUNT").append('(')
        if (column == null) {
            append('1')
        } else {
            appendExpression(column, params)
        }
        append(')').appendMain(params)
    }

    inline fun <reified T : Any> asExpression(): ColumnExpression<E, T> {
        require(columns.size == 1) { "This select expression has ${if (columns.isEmpty()) "dynamic" else "more then one"} columns" }
        val column = columns[0]
        if (column.sqlType.clazz != T::class.java) {
            throw IllegalArgumentException("The column type is not ${T::class.java}")
        }
        @Suppress("UNCHECKED_CAST")
        return column as ColumnExpression<E, T>
    }

    fun asQuerySource(alias: String?) =
        object : QuerySourceExpression<E>(alias) {
            override val columns = this@SelectExpression.columns

            override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
                append('(').appendExpression(this@SelectExpression, params).append(')')
                alias?.also { append(' ').appendRef(it) }
            }
        }

    @LorynDsl
    class Builder<E, T : QuerySourceExpression<E>>(private val from: T? = null) {
        private val columns: MutableList<ColumnExpression<E, *>> = mutableListOf()
        private var where: SqlExpression<Boolean>? = null
        private val groupBy: MutableList<ColumnExpression<E, *>> = mutableListOf()
        private var having: SqlExpression<Boolean>? = null
        private val orderBy: MutableList<OrderByExpression<E>> = mutableListOf()
        private var paginationParams: PaginationParams? = null
        private var distinct: Boolean = false
        private var createEntity: (() -> E)? = null

        fun column(column: ColumnExpression<E, *>) = also {
            this.columns += column.also { from?.checkColumn(it) }
        }

        fun columns(columns: List<ColumnExpression<E, *>>) = also {
            this.columns += columns.onEach { from?.checkColumn(it) }
        }

        fun columns(vararg columns: ColumnExpression<E, *>) = also {
            columns(columns.toList())
        }

        fun where(block: (T) -> SqlExpression<Boolean>) = also {
            this.where = block(from!!)
        }

        fun whereN(block: () -> SqlExpression<Boolean>) = also {
            this.where = block()
        }

        fun groupBy(column: ColumnExpression<E, *>) = also {
            this.groupBy += column.also { from?.checkColumn(it) }
        }

        fun groupBys(columns: List<ColumnExpression<E, *>>) = also {
            this.groupBy += columns.onEach { from?.checkColumn(it) }
        }

        fun groupBys(vararg columns: ColumnExpression<E, *>) = also {
            groupBys(columns.toList())
        }

        fun having(block: (T) -> SqlExpression<Boolean>) = also {
            this.having = block(from!!)
        }

        fun havingN(block: () -> SqlExpression<Boolean>) = also {
            this.having = block()
        }

        fun orderBy(orderBy: OrderByExpression<E>) = also {
            this.orderBy += orderBy
        }

        fun orderBys(orderBys: List<OrderByExpression<E>>) = also {
            this.orderBy += orderBys
        }

        fun orderBys(vararg orderBys: OrderByExpression<E>) = also {
            orderBys(orderBys.toList())
        }

        fun pagination(paginationParams: PaginationParams) = also {
            this.paginationParams = paginationParams
        }

        fun pagination(currentPage: Int, pageSize: Int) = also {
            pagination(PaginationParams(currentPage, pageSize))
        }

        fun limit(limit: Int) = also {
            pagination(PaginationParams(1, limit))
        }

        fun distinct(distinct: Boolean = true) = also {
            this.distinct = distinct
        }

        fun createEntity(createEntity: (() -> E)?) = also {
            this.createEntity = createEntity
        }

        fun build() =
            SelectExpression(columns, from, where, groupBy, having, orderBy, paginationParams, distinct, createEntity)
    }
}
