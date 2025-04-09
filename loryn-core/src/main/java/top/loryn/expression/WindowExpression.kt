package top.loryn.expression

import top.loryn.database.SqlBuilder

class WindowExpression<R : Any>(
    val function: FunctionExpression<R>,
    val partitionBy: List<ColumnExpression<*, *>>,
    val orderBy: List<OrderByExpression<*>>,
) : SqlExpression<R> {
    init {
        require(partitionBy.isNotEmpty() || orderBy.isNotEmpty()) { "Partition by or order by must be specified." }
    }

    override val sqlType = function.sqlType

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendExpression(function, params).append(' ').appendKeyword("OVER").append(' ').append('(')
        if (partitionBy.isNotEmpty()) {
            appendKeyword("PARTITION").append(' ').appendKeyword("BY").append(' ')
            appendExpressions(partitionBy, params)
            if (orderBy.isNotEmpty()) {
                append(' ')
            }
        }
        if (orderBy.isNotEmpty()) {
            appendKeyword("ORDER").append(' ').appendKeyword("BY").append(' ')
            appendExpressions(orderBy, params)
        }
        append(')')
    }
}
