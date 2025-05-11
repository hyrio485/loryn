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

    override val sqlType get() = sqlTypeNoNeed()

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.append(column, params, ignoreAlias = ignoreAlias)
        builder.append(' ').appendKeyword("=").append(' ')
        builder.append(value, params, ignoreAlias = ignoreAlias)
    }
}
