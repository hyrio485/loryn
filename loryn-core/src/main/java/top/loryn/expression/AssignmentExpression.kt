package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.schema.Column

data class AssignmentExpression<E, C : Any>(
    val column: ColumnExpression<E, C>,
    val value: SqlExpression<C>,
) : SqlExpression<Nothing> {
    init {
        if (column is Column<*, *> && column.notNull && value is ParameterExpression<*> && value.value == null) {
            throw IllegalArgumentException("The column $column cannot be null.")
        }
    }

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(column, params).append(' ').appendKeyword("=").append(' ').appendExpression(value, params)
    }
}
