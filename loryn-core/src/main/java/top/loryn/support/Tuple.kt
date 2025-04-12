package top.loryn.support

import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlExpression
import top.loryn.utils.SqlParamList

data class Tuple(val expressions: List<SqlExpression<*>>) : SqlExpression<Nothing> {
    constructor(vararg expressions: SqlExpression<*>) : this(expressions.toList())

    val size = expressions.size

    override fun SqlBuilder.appendSql(params: SqlParamList) =
        if (expressions.size == 1) {
            append(expressions[0], params)
        } else {
            append('(').append(expressions, params).append(')')
        }
}
