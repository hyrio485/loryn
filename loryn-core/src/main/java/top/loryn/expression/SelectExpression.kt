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
            appendExpressions(columns, params)
        } else {
            append('*')
        }
        appendMain(params)
        paginationParams?.also { append(' ').appendPagination(it) }
    }

    fun SqlBuilder.appendSqlCount(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("SELECT").append(' ').appendKeyword("COUNT").append("(1)").appendMain(params)
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
    class Builder<E, T : QuerySourceExpression<E>>(private val from: T) {
        private val columns: MutableList<ColumnExpression<E, *>> = mutableListOf()
        private var where: SqlExpression<Boolean>? = null
        private val groupBy: MutableList<ColumnExpression<E, *>> = mutableListOf()
        private var having: SqlExpression<Boolean>? = null
        private val orderBy: MutableList<OrderByExpression<E>> = mutableListOf()
        private var paginationParams: PaginationParams? = null
        private var distinct: Boolean = false
        private var createEntity: (() -> E)? = null

        fun addColumn(column: ColumnExpression<E, *>) {
            this.columns += column.also(from::checkColumn)
        }

        fun addColumns(columns: List<ColumnExpression<E, *>>) {
            this.columns += columns.onEach(from::checkColumn)
        }

        fun addColumns(vararg columns: ColumnExpression<E, *>) {
            addColumns(columns.toList())
        }

        fun where(block: (T) -> SqlExpression<Boolean>) {
            this.where = block(from)
        }

        fun addGroupBy(column: ColumnExpression<E, *>) {
            this.groupBy += column.also(from::checkColumn)
        }

        fun addGroupBys(columns: List<ColumnExpression<E, *>>) {
            this.groupBy += columns.onEach(from::checkColumn)
        }

        fun addGroupBys(vararg columns: ColumnExpression<E, *>) {
            addGroupBys(columns.toList())
        }

        fun having(block: (T) -> SqlExpression<Boolean>) {
            this.having = block(from)
        }

        fun addOrderBy(orderBy: OrderByExpression<E>) {
            this.orderBy += orderBy
        }

        fun addOrderBys(orderBys: List<OrderByExpression<E>>) {
            this.orderBy += orderBys
        }

        fun addOrderBys(vararg orderBys: OrderByExpression<E>) {
            addOrderBys(orderBys.toList())
        }

        fun pagination(paginationParams: PaginationParams) {
            this.paginationParams = paginationParams
        }

        fun pagination(currentPage: Int, pageSize: Int) {
            pagination(PaginationParams(currentPage, pageSize))
        }

        fun limit(limit: Int) {
            pagination(PaginationParams(1, limit))
        }

        fun distinct(distinct: Boolean = true) {
            this.distinct = distinct
        }

        fun createEntity(createEntity: (() -> E)?) {
            this.createEntity = createEntity
        }

        fun build() =
            SelectExpression(columns, from, where, groupBy, having, orderBy, paginationParams, distinct, createEntity)
    }
}
