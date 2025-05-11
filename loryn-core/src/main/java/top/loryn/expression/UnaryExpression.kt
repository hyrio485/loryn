package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

class UnaryExpression<T, R>(
    val operator: String,
    val expr: SqlExpression<T>,
    override val sqlType: SqlType<R>,
    val addParentheses: Boolean = true,
    val addWhiteSpace: Boolean = true,
) : SqlExpression<R> {
    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.appendKeyword(operator)
        if (addParentheses) {
            builder.append('(')
        } else if (addWhiteSpace) {
            builder.append(' ')
        }
        builder.append(expr, params, ignoreAlias = ignoreAlias)
        if (addParentheses) {
            builder.append(')')
        }
    }
}
