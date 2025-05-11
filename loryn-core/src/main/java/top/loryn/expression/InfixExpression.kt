package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

class InfixExpression<T1, T2, R>(
    val operators: List<String>,
    val expr1: SqlExpression<T1>,
    val expr2: SqlExpression<T2>,
    override val sqlType: SqlType<R>,
    val addParentheses: Boolean = false,
) : SqlExpression<R> {
    constructor(
        operator: String,
        expr1: SqlExpression<T1>,
        expr2: SqlExpression<T2>,
        sqlType: SqlType<R>,
        addParentheses: Boolean = false,
    ) : this(listOf(operator), expr1, expr2, sqlType, addParentheses)

    init {
        require(operators.isNotEmpty()) { "At least one operator must be provided" }
    }

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder
            .append(expr1, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
            .append(' ')
            .appendKeywords(operators, params, " ")
            .append(' ')
            .append(expr2, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
    }
}
