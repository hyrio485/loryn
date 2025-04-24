package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.BindableQuerySource
import top.loryn.schema.QuerySource
import top.loryn.support.LorynDsl
import top.loryn.support.PaginationParams
import top.loryn.support.WithAlias
import top.loryn.utils.SqlParamList
import top.loryn.utils.boxed

open class SelectExpression(
    open val columns: List<ColumnExpression<*>>,
    open val from: QuerySource?,
    val where: SqlExpression<Boolean>?,
    val groupBy: List<ColumnExpression<*>>,
    val having: SqlExpression<Boolean>?,
    val orderBy: List<OrderByExpression>,
    val paginationParams: PaginationParams?,
    val distinct: Boolean,
) : SqlExpression<Nothing> {
    override val sqlType get() = throw UnsupportedOperationException("SelectExpression does not have a sqlType")

    private fun SqlBuilder.buildSqlMain(params: SqlParamList) = also {
        from?.also {
            append(' ').appendKeyword("FROM").append(' ').append(it, params).appendAliasUsingAs(it)
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

    override fun buildSql(builder: SqlBuilder, params: SqlParamList) {
        builder.appendKeyword("SELECT").append(' ')
        if (distinct) {
            builder.appendKeyword("DISTINCT").append(' ')
        }
        if (columns.isNotEmpty()) {
            builder.appendList(columns, params) { column, params ->
                append(column.let { if (it is WithAlias) it.original else it }, params).appendAliasUsingAs(column)
            }
        } else {
            builder.append('*')
        }
        builder.buildSqlMain(params)
        paginationParams?.also { builder.append(' ').appendPagination(it) }
    }

    fun buildCountSql(builder: SqlBuilder, column: ColumnExpression<*>?, params: SqlParamList) {
        builder.appendKeyword("SELECT").append(' ').appendKeyword("COUNT").append('(')
        if (column == null) {
            builder.append('1')
        } else {
            builder.append(column, params)
        }
        builder.append(')').buildSqlMain(params)
    }

    inline fun <reified T> asExpression(): ColumnExpression<T> {
        // TODO：这里需要支持多列
        require(columns.size == 1) { "This select expression has ${if (columns.isEmpty()) "dynamic" else "more then one"} columns" }
        val column = columns[0]
        val boxedClass = T::class.java.boxed
        if (column.sqlType.clazz.boxed != boxedClass) {
            throw IllegalArgumentException("The column type is not $boxedClass")
        }
        @Suppress("UNCHECKED_CAST")
        return column as ColumnExpression<T>
    }

    fun asQuerySource(alias: String?): QuerySource =
        object : QuerySource, WithAlias {
            private val this0 = this@SelectExpression

            override val columns = this0.columns

            override val alias = alias
            override val original = this0

            // 因为这里是将子查询包装成了 QuerySource ，要在构建的SQL前后加括号（与其他情况的默认行为不同），因此要重写此方法。
            override fun buildSql(builder: SqlBuilder, params: SqlParamList) {
                builder.append(this, params, addParentheses = true)
            }
        }

    abstract class AbstractBuilder<T : QuerySource>(
        protected val from: T?,
    ) {
        protected var where: SqlExpression<Boolean>? = null
        protected val groupBy: MutableList<ColumnExpression<*>> = mutableListOf()
        protected var having: SqlExpression<Boolean>? = null
        protected val orderBy: MutableList<OrderByExpression> = mutableListOf()
        protected var paginationParams: PaginationParams? = null
        protected var distinct: Boolean = false

        fun where(where: SqlExpression<Boolean>) = also {
            this.where = where
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

        fun having(having: SqlExpression<Boolean>) = also {
            this.having = having
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
    }

    @LorynDsl
    open class Builder<T : QuerySource>(from: T? = null) : AbstractBuilder<T>(from) {
        protected val columns: MutableList<ColumnExpression<*>> = mutableListOf()

        open fun column(column: ColumnExpression<*>) = also {
            this.columns += column
        }

        open fun columns(columns: List<ColumnExpression<*>>) = also {
            this.columns += columns
        }

        open fun columns(vararg columns: ColumnExpression<*>) = also {
            columns(columns.toList())
        }

        open fun build() =
            SelectExpression(columns, from, where, groupBy, having, orderBy, paginationParams, distinct)
    }
}

fun <T : QuerySource> T.select(
    block: SelectExpression.Builder<T>.(T) -> Unit = {},
) = SelectExpression.Builder(this).apply { block(this@select) }.build()

class BindableSelectExpression<E>(
    override val columns: List<BindableColumnExpression<E, *>>,
    override val from: BindableQuerySource<E>,
    where: SqlExpression<Boolean>?,
    groupBy: List<ColumnExpression<*>>,
    having: SqlExpression<Boolean>?,
    orderBy: List<OrderByExpression>,
    paginationParams: PaginationParams?,
    distinct: Boolean,
) : SelectExpression(columns, from, where, groupBy, having, orderBy, paginationParams, distinct) {
    fun createEntity() = from.createEntity()

    class Builder<E, T : BindableQuerySource<E>>(from: T) : AbstractBuilder<T>(from) {
        private val columns: MutableList<BindableColumnExpression<E, *>> = mutableListOf()

        fun column(column: BindableColumnExpression<E, *>) = also {
            this.columns += column
        }

        fun columns(columns: List<BindableColumnExpression<E, *>>) = also {
            this.columns += columns
        }

        fun columns(vararg columns: BindableColumnExpression<E, *>) = also {
            columns(columns.toList())
        }

        fun build() =
            BindableSelectExpression(columns, from!!, where, groupBy, having, orderBy, paginationParams, distinct)
    }
}

fun <E, T : BindableQuerySource<E>> T.selectBindable(
    block: BindableSelectExpression.Builder<E, T>.(T) -> Unit = {},
) = BindableSelectExpression.Builder(this).apply { block(this@selectBindable) }.build()
