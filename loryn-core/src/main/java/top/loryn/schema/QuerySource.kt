package top.loryn.schema

import top.loryn.expression.ColumnExpression
import top.loryn.support.SqlAppender

interface QuerySource : SqlAppender {
    val columns: List<ColumnExpression<*>>
}
