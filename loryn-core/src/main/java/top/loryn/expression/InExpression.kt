package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.BooleanSqlType
import top.loryn.utils.SqlParamList

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

    override fun buildSql(builder: SqlBuilder, params: SqlParamList) {
        builder.append(expr, params).append(' ')
        if (not) {
            builder.appendKeyword("NOT").append(' ')
        }
        builder.appendKeyword("IN").append(" (")
        if (list != null) {
            builder.append(list, params)
        } else {
            builder.append(select!!, params)
        }
        builder.append(')')
    }
}
