package top.loryn.support

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

interface SqlAppender {
    fun SqlBuilder.appendSql(params: SqlParamList): SqlBuilder =
        throw UnsupportedOperationException("${javaClass.simpleName} does not have a SQL type.")
}
