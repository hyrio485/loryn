package top.loryn.expression

import top.loryn.database.SqlBuilder

abstract class ColumnExpression<T>(
    open val name: String? = null,
) : SqlExpression<T> {
    companion object {
        fun <T> wrap(expression: SqlExpression<T>) = object : ColumnExpression<T>() {
            override val sqlType = expression.sqlType

            override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) =
                append(expression, params)
        }
    }

    fun expr(value: T?) = ParameterExpression<T>(value, sqlType)
}
