package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.utils.SqlParamList

class WindowExpression<R>(
    val function: FunctionExpression<R>,
    val partitionBy: List<ColumnExpression<*>>,
    val orderBy: List<OrderByExpression>,
) : SqlExpression<R> {
    init {
        require(partitionBy.isNotEmpty() || orderBy.isNotEmpty()) { "Partition by or order by must be specified." }
    }

    override val sqlType = function.sqlType

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.append(function, params, ignoreAlias = ignoreAlias).append(' ')
        builder.appendKeyword("OVER").append(' ').append('(')
        if (partitionBy.isNotEmpty()) {
            builder.appendKeyword("PARTITION").append(' ').appendKeyword("BY").append(' ')
            builder.append(partitionBy, params, ignoreAlias = ignoreAlias)
            if (orderBy.isNotEmpty()) {
                builder.append(' ')
            }
        }
        if (orderBy.isNotEmpty()) {
            builder.appendKeyword("ORDER").append(' ').appendKeyword("BY").append(' ')
                .append(orderBy, params, ignoreAlias = ignoreAlias)
        }
        builder.append(')')
    }
}
