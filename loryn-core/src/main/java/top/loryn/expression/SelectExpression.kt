package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.QuerySource
import top.loryn.support.LorynDsl
import top.loryn.support.PaginationParams
import top.loryn.utils.SqlParamList

/**
 * SELECT表达式。
 */
class SelectExpression(
    val columns: List<ColumnExpression<*>>,
    val from: QuerySource?,
    val where: SqlExpression<Boolean>?,
    val groupBy: List<ColumnExpression<*>>,
    val having: SqlExpression<Boolean>?,
    val orderBy: List<OrderByExpression>,
    val paginationParams: PaginationParams?,
    val distinct: Boolean,
) : SqlExpression<Nothing> {
    private fun SqlBuilder.appendMain(params: SqlParamList) = also {
        from?.also {
            append(' ').appendKeyword("FROM").append(' ').append(it, params)
        }
        where?.also {
            append(' ').appendKeyword("WHERE").append(' ').append(it, params)
        }
        groupBy.takeIf { it.isNotEmpty() }?.also {
            append(' ').appendKeyword("GROUP").append(' ').appendKeyword("BY").append(' ').append(it, params)
        }
        having?.also {
            append(' ').appendKeyword("HAVING").append(' ').append(it, params)
        }
        orderBy.takeIf { it.isNotEmpty() }?.also {
            append(' ').appendKeyword("ORDER").append(' ').appendKeyword("BY").append(' ').append(it, params)
        }
    }

    override fun SqlBuilder.appendSql(params: SqlParamList) = also {
        appendKeyword("SELECT").append(' ')
        if (distinct) {
            appendKeyword("DISTINCT").append(' ')
        }
        if (columns.isNotEmpty()) {
            appendList(columns, params) { column, params ->
                with(column) {
                    appendSql(params).appendAlias(this) { append(' ').appendKeyword("AS").append(' ').appendRef(it) }
                }
            }
        } else {
            append('*')
        }
        appendMain(params)
        paginationParams?.also { append(' ').append(it) }
    }

    fun SqlBuilder.appendSqlCount(column: ColumnExpression<*>?, params: SqlParamList) = also {
        appendKeyword("SELECT").append(' ').appendKeyword("COUNT").append('(')
        if (column == null) {
            append('1')
        } else {
            append(column, params)
        }
        append(')').appendMain(params)
    }

    inline fun <reified T> asExpression(): ColumnExpression<T> {
        // TODO：这里需要支持多列
        require(columns.size == 1) { "This select expression has ${if (columns.isEmpty()) "dynamic" else "more then one"} columns" }
        val column = columns[0]
        if (column.sqlType.clazz != T::class.java) {
            throw IllegalArgumentException("The column type is not ${T::class.java}")
        }
        @Suppress("UNCHECKED_CAST")
        return column as ColumnExpression<T>
    }

    fun asQuerySource(alias: String?) =
        object : QuerySource {
            override val columns = this@SelectExpression.columns

            override fun SqlBuilder.appendSql(params: SqlParamList) = also {
                append('(').append(this@SelectExpression, params).append(')')
                alias?.also { append(' ').appendRef(it) }
            }
        }

    @LorynDsl
    class Builder<T : QuerySource>(private val from: T? = null) {
        private val columns: MutableList<ColumnExpression<*>> = mutableListOf()
        private var where: SqlExpression<Boolean>? = null
        private val groupBy: MutableList<ColumnExpression<*>> = mutableListOf()
        private var having: SqlExpression<Boolean>? = null
        private val orderBy: MutableList<OrderByExpression> = mutableListOf()
        private var paginationParams: PaginationParams? = null
        private var distinct: Boolean = false

        fun column(column: ColumnExpression<*>) = also {
            this.columns += column
        }

        fun columns(columns: List<ColumnExpression<*>>) = also {
            this.columns += columns
        }

        fun columns(vararg columns: ColumnExpression<*>) = also {
            columns(columns.toList())
        }

        fun where(block: (T) -> SqlExpression<Boolean>) = also {
            this.where = block(from!!)
        }

        fun whereN(block: () -> SqlExpression<Boolean>) = also {
            this.where = block()
        }

        fun groupBy(column: ColumnExpression<*>) = also {
            this.groupBy += column
        }

        fun groupBys(columns: List<ColumnExpression<*>>) = also {
            this.groupBy += columns
        }

        fun groupBys(vararg columns: ColumnExpression<*>) = also {
            groupBys(columns.toList())
        }

        fun having(block: (T) -> SqlExpression<Boolean>) = also {
            this.having = block(from!!)
        }

        fun havingN(block: () -> SqlExpression<Boolean>) = also {
            this.having = block()
        }

        fun orderBy(orderBy: OrderByExpression) = also {
            this.orderBy += orderBy
        }

        fun orderBys(orderBys: List<OrderByExpression>) = also {
            this.orderBy += orderBys
        }

        fun orderBys(vararg orderBys: OrderByExpression) = also {
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

        fun build() =
            SelectExpression(columns, from, where, groupBy, having, orderBy, paginationParams, distinct)
    }
}

fun <T : QuerySource> T.select(
    block: SelectExpression.Builder<T>.(T) -> Unit = {},
) = SelectExpression.Builder(this).apply { block(this@select) }.build()
