package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType

class UnaryExpression<T : Any, R : Any>(
    val operator: String,
    val expr: SqlExpression<T>,
    override val sqlType: SqlType<R>,
    val addParentheses: Boolean = true,
) : SqlExpression<R> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword(operator).append(' ')
        if (addParentheses) append('(')
        appendExpression(expr, params)
        if (addParentheses) append(')')
    }
}
