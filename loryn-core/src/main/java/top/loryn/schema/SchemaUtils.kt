package top.loryn.schema

fun checkTableColumn(table: Table<*>, column: Column<*, *>) {
    require(column.table === table) { "Column $column does not belong to table $table" }
    require(table.columns.any { it === column }) { "Column $column is not registered in table $table" }
}
