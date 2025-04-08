package top.loryn.expression

import top.loryn.schema.Column
import top.loryn.schema.Table

/**
 * 查询源表达式。
 *
 * @param E 绑定的查询结果的实体类型。
 */
abstract class QuerySourceExpression<E>(
    val alias: String?,
) : EntityCreator<E>, SqlExpression<Nothing> {
    abstract val columns: List<ColumnExpression<E, *>>

    open fun checkColumn(column: ColumnExpression<*, *>) {
        if (this !is Table<*> || column !is Column<*, *>) return
        require(column.root.table === this.root) { "Column $column does not belong to table $this" }
    }

    fun join(
        right: QuerySourceExpression<*>,
        joinType: JoinType = JoinType.DEFAULT,
        on: SqlExpression<Boolean>,
    ) = JoinExpression<Nothing>(this, right, joinType, on)

    fun <E1 : Any> join(
        right: QuerySourceExpression<*>,
        joinType: JoinType = JoinType.DEFAULT,
        on: SqlExpression<Boolean>,
        createEntity: (() -> E1)? = null,
    ) = JoinExpression<E1>(this, right, joinType, on, createEntity)
}
