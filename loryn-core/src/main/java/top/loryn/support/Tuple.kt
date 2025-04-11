package top.loryn.support

import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlExpression
import top.loryn.expression.SqlParam

data class Tuple(val expressions: List<SqlExpression<*>>) : SqlExpression<Nothing> {
    constructor(vararg expressions: SqlExpression<*>) : this(expressions.toList())

    val size = expressions.size

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        if (expressions.size == 1) {
            append(expressions[0], params)
        } else {
            append('(').append(expressions, params).append(')')
        }
    }
}
