package top.loryn.expression

import top.loryn.database.SqlBuilder

enum class OrderByType(val keyword: String) {
    ASC("ASC"),
    DESC("DESC"),
    ;
}

class OrderByExpression<E>(
    val column: ColumnExpression<E, *>,
    val type: OrderByType,
) : SqlExpression<Nothing> {
    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(column, params)
        if (type == OrderByType.DESC) {
            append(' ').appendKeyword(type.keyword)
        }
    }
}
