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

    private fun <C : Any> ColumnExpression<E, C>.withAlias() =
        if (alias == null) this else aliased(alias)

    @Suppress("UNCHECKED_CAST")
    operator fun <C : Any> get(column: ColumnExpression<E, C>): ColumnExpression<E, C> {
        var tableColumn = column.tableColumn ?: throw IllegalArgumentException("The table column of $column is null")
        var foundColumn = columns.find { it.tableColumn?.let { it === tableColumn } == true }
            ?: throw IllegalArgumentException("The column $column is not found in $this")
        return (foundColumn as ColumnExpression<E, C>).withAlias()
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <C : Any> get(index: Int) = (columns[index] as ColumnExpression<E, C>).withAlias()
}
