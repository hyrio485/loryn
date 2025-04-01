package top.loryn.expression

import top.loryn.database.SqlBuilder

data class NullSqlExpression<T : Any>(
    val label: String? = null,
) : SqlExpression<T> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("NULL")
    }
}
