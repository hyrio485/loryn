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
    val paginationParams: PaginationParams?,
    private val doCreateEntity: (() -> E)? = null,
) : EntityCreator<E>, SqlExpression<Nothing> {
    override fun createEntity() =
        if (doCreateEntity != null) {
            doCreateEntity()
        } else if (from != null) {
            from.createEntity()
        } else {
            super.createEntity()
        }

    private fun SqlBuilder.appendMain(params: MutableList<SqlParam<*>>) = also {
        from?.also { append(' ').appendKeyword("FROM").append(' ').appendExpression(it, params) }
        where?.also { append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params) }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("SELECT").append(' ')
        if (columns.isNotEmpty()) {
            appendExpressions(columns, params)
        } else {
            append('*')
        }
        appendMain(params)
        // todo append order by
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

    fun asQuerySource(alias: String?): QuerySourceExpression<E> {
        return object : QuerySourceExpression<E>() {
            override val columns = this@SelectExpression.columns

            override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
                append('(').appendExpression(this@SelectExpression, params).append(')')
                alias?.also { append(' ').appendRef(it) }
            }
        }
    }

    @LorynDsl
    class Builder<E, T : QuerySourceExpression<E>>(private val from: T) {
        private val columns: MutableList<ColumnExpression<E, *>> = mutableListOf()
        private var where: SqlExpression<Boolean>? = null
        private var paginationParams: PaginationParams? = null

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

        fun pagination(paginationParams: PaginationParams) {
            this.paginationParams = paginationParams
        }

        fun pagination(currentPage: Int, pageSize: Int) {
            pagination(PaginationParams(currentPage, pageSize))
        }

        fun limit(limit: Int) {
            pagination(PaginationParams(1, limit))
        }

        fun build() = SelectExpression(columns, from, where, paginationParams)
    }
}
