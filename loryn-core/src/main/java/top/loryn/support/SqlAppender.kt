package top.loryn.support

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

interface SqlAppender {
    fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean)
}
