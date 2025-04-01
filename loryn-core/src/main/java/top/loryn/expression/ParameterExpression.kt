package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType

class ParameterExpression<T : Any>(
    val value: T?,
    override val sqlType: SqlType<T>,
) : SqlExpression<T> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        append("?")
        params += SqlParam(value, sqlType)
    }
}
