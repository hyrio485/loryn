package top.loryn.expression

import top.loryn.database.SqlBuilder

data class Tuple(val expressions: List<SqlExpression<*>>) : SqlExpression<Nothing> {
    constructor(vararg expressions: SqlExpression<*>) : this(expressions.toList())

    val size = expressions.size

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        if (expressions.size == 1) {
            appendExpression(expressions[0], params)
        } else {
            append('(').appendExpressions(expressions, params).append(')')
        }
    }
}
