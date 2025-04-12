package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

class ParameterExpression<T>(
    val value: T?,
    override val sqlType: SqlType<T>,
) : SqlExpression<T> {
    override fun SqlBuilder.appendSql(params: SqlParamList) = also {
        append("?")
        params += SqlParam(value, sqlType)
    }
}
