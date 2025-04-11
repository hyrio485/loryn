package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.BooleanSqlType

class InExpression(
    val expr: SqlExpression<*>,
    val list: List<SqlExpression<*>>? = null,
    val select: SelectExpression? = null,
    val not: Boolean = false,
) : SqlExpression<Boolean> {
    init {
        require(list == null && select != null || list != null && select == null) {
            "Either list or select must be provided, but not both."
        }
    }

    override val sqlType = BooleanSqlType

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        append(expr, params).append(' ')
        if (not) appendKeyword("NOT").append(' ')
        appendKeyword("IN").append(" (")
        if (list != null) {
            append(list, params)
        } else {
            append(select!!, params)
        }
        append(')')
    }
}
