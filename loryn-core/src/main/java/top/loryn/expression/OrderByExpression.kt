package top.loryn.expression

import top.loryn.database.SqlBuilder

enum class OrderByType(val keyword: String) {
    ASC("ASC"),
    DESC("DESC"),
    ;
}

class OrderByExpression(
    val column: ColumnExpression<*>,
    val type: OrderByType,
) : SqlExpression<Nothing> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        append(column, params)
        if (type == OrderByType.DESC) {
            append(' ').appendKeyword(type.keyword)
        }
    }
}
