package top.loryn.expression

import top.loryn.schema.Column
import top.loryn.schema.Table

/**
 * 查询源表达式。
 *
 * @param E 绑定的查询结果的实体类型。
 */
abstract class QuerySourceExpression<E> : EntityCreator<E>, SqlExpression<Nothing> {
    abstract val columns: List<ColumnExpression<E, *>>

    open fun checkColumn(column: ColumnExpression<*, *>) {
        val isTable = this is Table<*>
        require(columns.any { it === column }) {
            "Column $column is not registered in ${if (isTable) "table" else "query source"} $this"
        }
        if (column is Column<*, *> && isTable) {
            require(column.table === this) { "Column $column does not belong to table $this" }
        }
    }
}
