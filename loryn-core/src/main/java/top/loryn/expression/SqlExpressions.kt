package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.Table

abstract class ColumnExpression<T : Any>(val sqlTypeNullable: SqlType<T>?, val label: String?) : SqlExpression<T> {
    override val sqlType by lazy(LazyThreadSafetyMode.NONE) { sqlTypeNullable ?: super.sqlType }

    open fun SqlBuilder.appendSqlInSelectClause(params: MutableList<SqlParam<*>>) = also {
        appendSql(params)
        if (label != null) {
            append(' ').appendKeyword("AS").append(' ').appendRef(label)
        }
    }

    abstract fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>): SqlBuilder

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        if (label != null) {
            appendRef(label)
        } else {
            appendSqlOriginal(params)
        }
    }
}

class ParameterExpression<T : Any>(
    val value: T?, sqlType: SqlType<T>, label: String? = null,
) : ColumnExpression<T>(sqlType, label) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        append("?")
        params += SqlParam(value, sqlType)
    }
}

class UnaryExpression<T : Any, R : Any>(
    val operator: String, val expr: SqlExpression<T>,
    sqlType: SqlType<R>, val addParentheses: Boolean = true, label: String? = null,
) : ColumnExpression<R>(sqlType, label) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        appendKeyword(operator).append(' ')
        if (addParentheses) append('(')
        appendExpression(expr, params)
        if (addParentheses) append(')')
    }
}

class BinaryExpression<T1 : Any, T2 : Any, R : Any>(
    val operator: String, val expr1: SqlExpression<T1>, val expr2: SqlExpression<T2>,
    sqlType: SqlType<R>, val addParentheses: Boolean = true, label: String? = null,
) : ColumnExpression<R>(sqlType, label) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        if (addParentheses) append('(')
        appendExpression(expr1, params)
        if (addParentheses) append(')')
        append(' ').appendKeyword(operator).append(' ')
        if (addParentheses) append('(')
        appendExpression(expr2, params)
        if (addParentheses) append(')')
    }
}

data class AssignmentExpression<T : Any>(
    val column: ColumnExpression<T>, val value: SqlExpression<T>,
) : SqlExpression<Nothing> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(column, params).append(' ').appendKeyword("=").append(' ').appendExpression(value, params)
    }
}

abstract class QuerySourceExpression : SqlExpression<Nothing>

data class TableExpression(
    val table: Table<*>, val alias: String? = null,
) : QuerySourceExpression() {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendTable(table)
        alias?.also { append(' ').appendRef(it) }
    }
}

class SelectExpression<T : Any>(
    val columns: List<ColumnExpression<*>>,
    val from: QuerySourceExpression?,
    val where: SqlExpression<Boolean>?,
    sqlType: SqlType<T>? = null,
    label: String? = null,
) : ColumnExpression<T>(sqlType?.also {
    if (columns.size != 1) {
        throw IllegalArgumentException("The sqlType argument can only be used when there is exactly one column.")
    }
}, label) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("SELECT").append(' ')
        if (columns.isNotEmpty()) {
            columns.forEachIndexed { index, column ->
                if (index > 0) append(", ")
                column.run { appendSqlInSelectClause(params) }
            }
        } else {
            append('*')
        }
        if (from != null) {
            append(' ').appendKeyword("FROM").append(' ').appendExpression(from, params)
        }
        where?.also {
            append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params)
        }
    }
}
