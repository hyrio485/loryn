package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

class UnaryExpression<T, R>(
    val operator: String,
    val expr: SqlExpression<T>,
    override val sqlType: SqlType<R>,
    val addParentheses: Boolean = true,
) : SqlExpression<R> {
    override fun SqlBuilder.appendSql(params: SqlParamList) = also {
        appendKeyword(operator).append(if (addParentheses) '(' else ' ').append(expr, params)
        if (addParentheses) append(')')
    }
}
