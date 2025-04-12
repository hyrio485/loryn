package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

class NullSqlExpression<T> : SqlExpression<T> {
    override fun SqlBuilder.appendSql(params: SqlParamList) = appendKeyword("NULL")
}
