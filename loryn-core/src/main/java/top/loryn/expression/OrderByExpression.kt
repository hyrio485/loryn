package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

class OrderByExpression(
    val column: ColumnExpression<*>,
    val type: OrderByType,
) : SqlExpression<Nothing> {
    constructor(column: ColumnExpression<*>, ascending: Boolean) : this(
        column,
        if (ascending) OrderByType.ASC else OrderByType.DESC
    )

    override val sqlType get() = throw UnsupportedOperationException("OrderByExpression does not have a sqlType")

    enum class OrderByType(val keyword: String) {
        ASC("ASC"),
        DESC("DESC"),
        ;
    }

    override fun SqlBuilder.appendSql(params: SqlParamList) = also {
        append(column, params)
        if (type == OrderByType.DESC) {
            append(' ').appendKeyword(type.keyword)
        }
    }
}
