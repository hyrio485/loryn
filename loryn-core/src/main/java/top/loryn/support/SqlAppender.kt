package top.loryn.support

import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlParam

interface SqlAppender {
    fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>): SqlBuilder
}
