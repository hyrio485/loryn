package top.loryn.schema

import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlExpression
import top.loryn.schema.JoinQuerySource.JoinType
import top.loryn.support.SqlAppender

interface QuerySource : SqlAppender {
    val columns: List<ColumnExpression<*>>

    fun join(right: QuerySource, joinType: JoinType, on: SqlExpression<Boolean>) =
        JoinQuerySource(this, right, joinType, on)

    fun leftJoin(right: QuerySource, on: SqlExpression<Boolean>) =
        JoinQuerySource(this, right, JoinType.LEFT, on)
}
