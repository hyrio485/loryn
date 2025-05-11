package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

class FunctionExpression<R>(
    val name: String,
    override val sqlType: SqlType<R>,
    val args: List<SqlExpression<*>> = emptyList(),
    val addParenthesesWhenNoArgs: Boolean = true,
) : SqlExpression<R> {
    constructor(name: String, sqlType: SqlType<R>, vararg args: SqlExpression<*>) : this(name, sqlType, args.toList())

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.appendKeyword(name)
        if (args.isEmpty()) {
            if (addParenthesesWhenNoArgs) {
                builder.append("()")
            }
        } else {
            builder.append(args, params, addParentheses = true, ignoreAlias = ignoreAlias)
        }
    }
}
