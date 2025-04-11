//package top.loryn.dialect.mysql
//
//import top.loryn.database.Database
//import top.loryn.database.SqlBuilder
//import top.loryn.expression.*
//import top.loryn.schema.TableColumn
//import top.loryn.schema.Table
//import top.loryn.statement.BaseInsertStatement
//import top.loryn.statement.ColumnSelectionBuilder
//import top.loryn.statement.StatementBuilder
//import top.loryn.statement.selectColumns
//import top.loryn.support.LorynDsl
//
//class InsertOrUpdateStatement<E>(
//    database: Database,
//    table: Table<E>,
//    columns: List<ColumnExpression<E, *>>,
//    val values: List<ParameterExpression<*>>,
//    val sets: List<AssignmentExpression<E, *>>,
//    useGeneratedKeys: Boolean = false,
//) : BaseInsertStatement<E>(database, table, columns, useGeneratedKeys) {
//    init {
//        require(columns.isNotEmpty()) { "At least one column must be set" }
//        if (values.size != columns.size) {
//            throw IllegalArgumentException("The number of values must match the number of columns")
//        }
//        require(sets.isNotEmpty()) { "At least one column for update must be set" }
//    }
//
//    override fun SqlBuilder.doGenerateSql(params: MutableList<SqlParam<*>>) = also {
//        appendInsertIntoColumns(params).appendKeyword("VALUES")
//        append(' ').appendRowValues(values, params)
//        append(' ').appendKeyword("ON")
//        append(' ').appendKeyword("DUPLICATE")
//        append(' ').appendKeyword("KEY")
//        append(' ').appendKeyword("UPDATE")
//        append(' ').appends(sets, params)
//    }
//}
//
//@LorynDsl
//class InsertOrUpdateBuilder<E, T : Table<E>>(
//    table: T, val useGeneratedKeys: Boolean = false,
//) : StatementBuilder<T, InsertOrUpdateStatement<E>>(table) {
//    private val columns = mutableListOf<ColumnExpression<E, *>>()
//    private val values = mutableListOf<ParameterExpression<*>>()
//    private val sets = mutableListOf<AssignmentExpression<E, *>>()
//
//    fun <C> assign(column: TableColumn<E, C>, value: C?) {
//        if (column.notNull && value == null) {
//            throw IllegalArgumentException("Column ${column.name} cannot be null")
//        }
//        columns += column.also(table::checkColumn)
//        values += column.expr(value)
//    }
//
//    fun <C> set(column: TableColumn<E, C>, value: SqlExpression<C>) {
//        require(column.also(table::checkColumn) in columns) { "Column $column is not in the insert columns" }
//        sets += AssignmentExpression(column, value)
//    }
//
//    fun <C> set(column: TableColumn<E, C>, block: (TableColumn<E, C>) -> SqlExpression<C>) {
//        set(column, block(column))
//    }
//
//    fun <C> set(column: TableColumn<E, C>, value: C?) {
//        set(column, column.expr(value))
//    }
//
//    override fun buildStatement(database: Database) =
//        InsertOrUpdateStatement(database, table, columns, values, sets, useGeneratedKeys)
//}
//
//fun <E, T : Table<E>> Database.insertOrUpdate(
//    table: T,
//    useGeneratedKeys: Boolean = false,
//    block: InsertOrUpdateBuilder<E, T>.(T) -> Unit = {},
//) = InsertOrUpdateBuilder(table, useGeneratedKeys).apply { block(table) }.buildStatement(this)
//
//// region 插入或更新实体
//
//fun <E, T : Table<E>> Database.insertOrUpdate(
//    table: T,
//    entity: E,
//    useGeneratedKeys: Boolean = false,
//    columns: List<TableColumn<E, *>> = table.insertColumns,
//    updateColumns: List<TableColumn<E, *>> = table.updateColumns,
//): Int {
//    val columns = columns.onEach(table::checkColumn)
//    return InsertOrUpdateStatement(
//        this, table, columns,
//        columns.map { it.getValueExpr(entity) },
//        updateColumns.map { it.assignByEntity(entity) },
//        useGeneratedKeys = useGeneratedKeys,
//    ).let { it.execute { rs -> it.fillInPrimaryKeysForEachRow(entity, rs) } }
//}
//
//fun <E, T : Table<E>> Database.insertOrUpdate(
//    table: T,
//    entity: E,
//    useGeneratedKeys: Boolean = false,
//    columnsSelector: ColumnSelectionBuilder<E>.(T) -> Unit,
//    updateColumnsSelector: ColumnSelectionBuilder<E>.(T) -> Unit,
//) = insertOrUpdate(
//    table,
//    entity,
//    useGeneratedKeys,
//    table.selectColumns(columnsSelector),
//    table.selectColumns(updateColumnsSelector),
//)
//
//// endregion
