package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.BindableQuerySource
import top.loryn.schema.QuerySource
import top.loryn.statement.WhereClause
import top.loryn.support.LorynDsl
import top.loryn.support.PaginationParams
import top.loryn.support.SqlType
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
    override val sqlType get() = sqlTypeNoNeed()

    private fun SqlBuilder.buildSqlMain(params: SqlParamList) {
        from?.also {
            append(' ').appendKeyword("FROM").append(' ').appendAndAsAlias(it, params)
        }
        where?.also {
            append(' ').appendKeyword("WHERE").append(' ').append(it, params, ignoreAlias = true)
        }
        groupBy.takeIf { it.isNotEmpty() }?.also { list ->
            append(' ').appendKeyword("GROUP").append(' ').appendKeyword("BY").append(' ')
            appendList(list, params) { column, params ->
                append(column, params)
            }
        }
        having?.also {
            append(' ').appendKeyword("HAVING").append(' ').append(it, params)
        }
        orderBy.takeIf { it.isNotEmpty() }?.also {
            append(' ').appendKeyword("ORDER").append(' ').appendKeyword("BY").append(' ').append(it, params)
        }
    }

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.appendKeyword("SELECT").append(' ')
        if (distinct) {
            builder.appendKeyword("DISTINCT").append(' ')
        }
        if (columns.isNotEmpty()) {
            builder.appendList(columns, params) { column, params ->
                appendAndAsAlias(column, params)
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
            builder.append(column, params, ignoreAlias = true)
        }
        builder.append(')').buildSqlMain(params)
    }

    @PublishedApi
    internal inline fun <reified T> checkAndGetFirstColumnSqlType(): SqlType<T> {
        require(columns.size == 1) { "Only select expression with one column can be converted to ColumnExpression" }
        val column = columns[0]
        val boxedClass = T::class.java.boxed
        if (column.sqlType.clazz.boxed != boxedClass) {
            throw IllegalArgumentException("The column type is not $boxedClass")
        }
        @Suppress("UNCHECKED_CAST")
        return column.sqlType as SqlType<T>
    }

    inline fun <reified T> asColumn(): ColumnExpression<T> {
        require(columns.size == 1) { "Only select expression with one column can be converted to ColumnExpression" }
        checkAndGetFirstColumnSqlType<T>()
        @Suppress("UNCHECKED_CAST")
        return columns[0] as ColumnExpression<T>
    }

    inline fun <reified T> asExpression(sqlType: SqlType<T>? = null) = object : SqlExpression<T> {
        // TODO: 这里需要支持多列
        override val sqlType: SqlType<T>
            get() {
                return if (sqlType == null) {
                    require(columns.size != 1) { "When the sqlType is null, the columns size must be 1" }
                    checkAndGetFirstColumnSqlType<T>()
                } else {
                    sqlType
                }
            }

        override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
            builder.append('(').append(this@SelectExpression, params, ignoreAlias = ignoreAlias).append(')')
        }
    }

    fun asQuerySource(alias: String? = null): QuerySource =
        object : QuerySource, WithAlias {
            private val this0 = this@SelectExpression

            override val columns = this0.columns

            override val alias = alias
            override val original = this0

            // 因为这里是将子查询包装成了 QuerySource ，要在构建的SQL前后加括号（与其他情况的默认行为不同），因此要重写此方法。
            override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
                builder.append(original, params, addParentheses = true, ignoreAlias = ignoreAlias)
            }
        }

    abstract class AbstractBuilder<T : QuerySource>(
        protected val from: T?,
    ) : WhereClause {
        protected var where: SqlExpression<Boolean>? = null
        protected val groupBy: MutableList<ColumnExpression<*>> = mutableListOf()
        protected var having: SqlExpression<Boolean>? = null
        protected val orderBy: MutableList<OrderByExpression> = mutableListOf()
        protected var paginationParams: PaginationParams? = null
        protected var distinct: Boolean = false

        override fun where(where: SqlExpression<Boolean>) {
            this.where = where
        }

        fun groupBy(columns: List<ColumnExpression<*>>) {
            this.groupBy += columns
        }

        fun groupBy(vararg columns: ColumnExpression<*>) {
            groupBy(columns.toList())
        }

        fun having(having: SqlExpression<Boolean>) {
            this.having = having
        }

        // region order by

        fun orderBy(orderBys: List<OrderByExpression>) {
            this.orderBy += orderBys
        }

        fun orderBy(vararg orderBys: OrderByExpression) {
            orderBy(orderBys.toList())
        }

        @JvmName("orderByColumn")
        fun orderBy(orderBys: List<ColumnExpression<*>>, type: OrderByExpression.OrderByType) {
            orderBy(orderBys.map { it.toOrderBy(type) })
        }

        @JvmName("orderByColumn")
        fun orderBy(vararg orderBys: ColumnExpression<*>, type: OrderByExpression.OrderByType) {
            orderBy(orderBys.toList(), type)
        }

        @JvmName("orderByColumn")
        fun orderBy(orderBys: List<ColumnExpression<*>>, ascending: Boolean = true) {
            orderBy(orderBys.map { it.toOrderBy(ascending) })
        }

        @JvmName("orderByColumn")
        fun orderBy(vararg orderBys: ColumnExpression<*>, ascending: Boolean = true) {
            orderBy(orderBys.toList(), ascending)
        }

        @JvmName("orderByExpr")
        fun orderBy(orderBys: List<SqlExpression<*>>, type: OrderByExpression.OrderByType) {
            orderBy(orderBys.map { it.toOrderBy(type) })
        }

        @JvmName("orderByExpr")
        fun orderBy(vararg orderBys: SqlExpression<*>, type: OrderByExpression.OrderByType) {
            orderBy(orderBys.toList(), type)
        }

        @JvmName("orderByExpr")
        fun orderBy(orderBys: List<SqlExpression<*>>, ascending: Boolean = true) {
            orderBy(orderBys.map { it.toOrderBy(ascending) })
        }

        @JvmName("orderByExpr")
        fun orderBy(vararg orderBys: SqlExpression<*>, ascending: Boolean = true) {
            orderBy(orderBys.toList(), ascending)
        }

        // endregion

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

        fun countStar(alias: String? = null) = StarSqlExpression.count().asColumn(alias)

        fun count1(alias: String? = null) = 1.toSqlParam().count().asColumn(alias)
    }

    @LorynDsl
    open class Builder<T : QuerySource>(from: T? = null) : AbstractBuilder<T>(from) {
        protected val columns: MutableList<ColumnExpression<*>> = mutableListOf()

        open fun column(columns: List<ColumnExpression<*>>) {
            this.columns += columns
        }

        open fun column(vararg columns: ColumnExpression<*>) {
            column(columns.toList())
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

        fun column(columns: List<BindableColumnExpression<E, *>>) {
            this.columns += columns
        }

        fun column(vararg columns: BindableColumnExpression<E, *>) {
            column(columns.toList())
        }

        fun build() =
            BindableSelectExpression(columns, from!!, where, groupBy, having, orderBy, paginationParams, distinct)
    }
}

fun <E, T : BindableQuerySource<E>> T.selectBindable(
    block: BindableSelectExpression.Builder<E, T>.(T) -> Unit = {},
) = BindableSelectExpression.Builder(this).apply { block(this@selectBindable) }.build()
