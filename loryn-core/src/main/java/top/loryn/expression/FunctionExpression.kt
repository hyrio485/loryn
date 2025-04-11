package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType

class FunctionExpression<R>(
    val name: String,
    override val sqlType: SqlType<R>,
    val args: List<SqlExpression<*>> = emptyList(),
    val addParenthesesWhenNoArgs: Boolean = true,
) : SqlExpression<R> {
    constructor(name: String, sqlType: SqlType<R>, vararg args: SqlExpression<*>) : this(name, sqlType, args.toList())

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword(name)
        if (args.isEmpty()) {
            if (addParenthesesWhenNoArgs) {
                append("()")
            }
        } else {
            append('(').append(args, params).append(')')
        }
    }
}
