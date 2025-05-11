package top.loryn.support

import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlExpression
import top.loryn.utils.SqlParamList

data class Tuple(val expressions: List<SqlExpression<*>>) : SqlExpression<Nothing> {
    constructor(vararg expressions: SqlExpression<*>) : this(expressions.toList())

    override val sqlType get() = sqlTypeNoNeed()

    val size = expressions.size

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        if (expressions.size == 1) {
            builder.append(expressions[0], params, ignoreAlias = ignoreAlias)
        } else {
            builder.append(expressions, params, addParentheses = true, ignoreAlias = ignoreAlias)
        }
    }
}
