package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.Column
import top.loryn.utils.SqlParamList

data class AssignmentExpression<C>(
    val column: ColumnExpression<C>,
    val value: SqlExpression<C>,
) : SqlExpression<Nothing> {
    init {
        if (column is Column<*> && column.notNull && value is SqlParam<*> && value.value == null) {
            throw IllegalArgumentException("The column $column cannot be null.")
        }
    }

    override val sqlType get() = throw UnsupportedOperationException("AssignmentExpression does not have a sqlType")

    override fun buildSql(builder: SqlBuilder, params: SqlParamList) {
        builder.append(column, params).append(' ').appendKeyword("=").append(' ').append(value, params)
    }
}
