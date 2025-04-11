package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType

class InfixExpression<T1, T2, R>(
    val operators: List<String>,
    val expr1: SqlExpression<T1>,
    val expr2: SqlExpression<T2>,
    override val sqlType: SqlType<R>,
    val addParentheses: Boolean = false,
) : SqlExpression<R> {
    init {
        require(operators.isNotEmpty()) { "At least one operator must be provided" }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        if (addParentheses) append('(')
        append(expr1, params)
        if (addParentheses) append(')')
        append(' ').appendList(operators, params, " ") { operator, _ ->
            appendKeyword(operator)
        }.append(' ')
        if (addParentheses) append('(')
        append(expr2, params)
        if (addParentheses) append(')')
    }
}
