package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.Column

abstract class ColumnExpression<T : Any>(
    val sqlTypeNullable: SqlType<T>?,
    val alias: String?,
) : SqlExpression<T> {
    override val sqlType: SqlType<T>
        get() = sqlTypeNullable
            ?: throw UnsupportedOperationException("This column expression does not have a SQL type.")

    open fun SqlBuilder.appendSqlInSelectClause(params: MutableList<SqlParam<*>>) = also {
        appendSql(params)
        if (alias != null) {
            append(' ').appendKeyword("AS").append(' ').appendRef(alias)
        }
    }

    abstract fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>): SqlBuilder

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        if (alias != null) {
            appendRef(alias)
        } else {
            appendSqlOriginal(params)
        }
    }
}

class ParameterExpression<T : Any>(
    val value: T?,
    sqlType: SqlType<T>,
    label: String? = null,
) : ColumnExpression<T>(sqlType, label) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        append("?")
        params += SqlParam(value, sqlType)
    }
}

class UnaryExpression<T : Any, R : Any>(
    val operator: String,
    val expr: SqlExpression<T>,
    sqlType: SqlType<R>,
    val addParentheses: Boolean = true,
    label: String? = null,
) : ColumnExpression<R>(sqlType, label) {
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        appendKeyword(operator).append(' ')
        if (addParentheses) append('(')
        appendExpression(expr, params)
        if (addParentheses) append(')')
    }
}

class BinaryExpression<T1 : Any, T2 : Any, R : Any>(
    val operator: String,
    val expr1: SqlExpression<T1>,
    val expr2: SqlExpression<T2>,
    sqlType: SqlType<R>,
    val addParentheses: Boolean = true,
    label: String? = null,
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
    val column: ColumnExpression<T>,
    val value: SqlExpression<T>,
) : SqlExpression<Nothing> {
    init {
        if (column is Column<*> && column.notNull && value is ParameterExpression && value.value == null) {
            throw IllegalArgumentException("The column $column cannot be null.")
        }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(column, params).append(' ').appendKeyword("=").append(' ').appendExpression(value, params)
    }
}

abstract class QuerySourceExpression : SqlExpression<Nothing> {
    abstract val columns: List<ColumnExpression<*>>
}

class SelectExpression(
    override val columns: List<ColumnExpression<*>>,
    val from: QuerySourceExpression?,
    val where: SqlExpression<Boolean>?,
) : QuerySourceExpression() {
    inline fun <reified T : Any> asColumn(): ColumnExpression<T> {
        require(columns.size == 1) { "This select expression has ${if (columns.isEmpty()) "dynamic" else "more then one"} columns" }
        val column = columns[0]
        if (column.sqlType.clazz != T::class.java) {
            throw IllegalArgumentException("The column type is not ${T::class.java}")
        }
        @Suppress("UNCHECKED_CAST")
        return column as ColumnExpression<T>
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
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
