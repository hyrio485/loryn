package top.loryn.expression

import top.loryn.database.SqlBuilder

class NullSqlExpression<T : Any> : SqlExpression<T> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = appendKeyword("NULL")
}
