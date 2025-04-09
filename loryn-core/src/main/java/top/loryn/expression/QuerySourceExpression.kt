package top.loryn.expression

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
        if (this !is Table<*>) return
        val root = root
        val tableColumn = column.tableColumn ?: return
        require(root === tableColumn.table) { "Column $column does not belong to table $this" }
        require(root.columns.any { it === tableColumn }) { "Column $column does not registered in table $this" }
    }

    fun <E1 : Any> join(
        right: QuerySourceExpression<*>,
        joinType: JoinType,
        on: SqlExpression<Boolean>,
        createEntity: (() -> E1)? = null,
    ) = JoinExpression<E1>(this, right, joinType, on, createEntity)

    fun <E1 : Any> leftJoin(
        right: QuerySourceExpression<*>,
        on: SqlExpression<Boolean>,
        createEntity: (() -> E1)? = null,
    ) = JoinExpression<E1>(this, right, JoinType.LEFT, on, createEntity)

    fun join(right: QuerySourceExpression<*>, joinType: JoinType, on: SqlExpression<Boolean>) =
        join<Nothing>(right, joinType, on, null)

    fun leftJoin(right: QuerySourceExpression<*>, on: SqlExpression<Boolean>) =
        leftJoin<Nothing>(right, on, null)
}
