package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

class TernaryExpression<T1, T2, T3, R>(
    val operators1: List<String>,
    val operators2: List<String>,
    val expr1: SqlExpression<T1>,
    val expr2: SqlExpression<T2>,
    val expr3: SqlExpression<T3>,
    override val sqlType: SqlType<R>,
    val addParentheses: Boolean = false,
) : SqlExpression<R> {
    constructor(
        operator1: String,
        operator2: String,
        expr1: SqlExpression<T1>,
        expr2: SqlExpression<T2>,
        expr3: SqlExpression<T3>,
        sqlType: SqlType<R>,
        addParentheses: Boolean = false,
    ) : this(listOf(operator1), listOf(operator2), expr1, expr2, expr3, sqlType, addParentheses)

    init {
        require(operators1.isNotEmpty()) { "At least one operator of operators1 must be provided" }
        require(operators2.isNotEmpty()) { "At least one operator of operators1 must be provided" }
    }

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder
            .append(expr1, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
            .append(' ').appendKeywords(operators1, params, " ").append(' ')
            .append(expr2, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
            .append(' ').appendKeywords(operators2, params, " ").append(' ')
            .append(expr3, params, addParentheses = addParentheses, ignoreAlias = ignoreAlias)
    }
}
