package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.*
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.support.LorynDsl
import top.loryn.support.Tuple

class UpdateStatement<E>(
    database: Database,
    val table: Table<E>,
    val sets: List<AssignmentExpression<E, *>>,
    val where: SqlExpression<Boolean>?,
) : DmlStatement(database) {
    init {
        require(sets.isNotEmpty()) { "At least one column must be set" }
    }

    override fun SqlBuilder.doGenerateSql(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("UPDATE").append(' ').appendTable(table).append(' ')
        appendKeyword("SET").append(' ').appendExpressions(sets, params)
        where?.also { append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params) }
    }
}

@LorynDsl
class UpdateBuilder<E, T : Table<E>>(table: T) : StatementBuilder<T, UpdateStatement<E>>(table) {
    private val sets = mutableListOf<AssignmentExpression<E, *>>()
    private var where: SqlExpression<Boolean>? = null

    fun <C : Any> set(column: Column<E, C>, value: SqlExpression<C>) {
        sets += AssignmentExpression(column.also(table::checkColumn), value)
    }

    fun <C : Any> set(column: Column<E, C>, block: (Column<E, C>) -> SqlExpression<C>) {
        set(column, block(column))
    }

    fun <C : Any> set(column: Column<E, C>, value: C?) {
        set(column, column.expr(value))
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun buildStatement(database: Database) = UpdateStatement(database, table, sets, where)
}

fun <E, T : Table<E>> Database.update(
    table: T,
    block: UpdateBuilder<E, T>.(T) -> Unit = {},
) = UpdateBuilder(table).apply { block(table) }.buildStatement(this).execute()

// region 更新实体

fun <E, T : Table<E>> Database.update(
    table: T,
    entity: E,
    columns: List<Column<E, *>> = table.updateColumns,
) = update(table) {
    columns.forEach { set(it) { it.getValueExpr(entity) } }
    where { it.primaryKeyFilter(entity) }
}

fun <E, T : Table<E>> Database.update(
    table: T,
    entity: E,
    vararg columns: Column<E, *>,
) = update(table, entity, columns.toList())

fun <E, T : Table<E>> Database.update(
    table: T,
    entity: E,
    columnsSelector: ColumnSelectionBuilder<E>.(T) -> Unit,
) = update(table, entity, table.selectColumns(columnsSelector))

// endregion

// region 乐观锁更新实体

fun <E, T : Table<E>> Database.updateOptimistic(
    table: T,
    entity: E,
    revColumn: Column<E, Int> = table.revColumn,
    columns: List<Column<E, *>> = table.updateColumns,
): Int {
    require(revColumn !in columns) { "Revision column $revColumn cannot be included in the update columns" }
    return update(table) {
        columns.forEach { column ->
            set(column) { it.getValueExpr(entity) }
            set(revColumn, revColumn.getValue(entity)!! + 1)
        }
        where { it.primaryKeyFilter(entity) and (revColumn eq revColumn.getValueExpr(entity)) }
    }
}

fun <E, T : Table<E>> Database.updateOptimistic(
    table: T,
    entity: E,
    revColumn: Column<E, Int> = table.revColumn,
    vararg columns: Column<E, *>,
) = updateOptimistic(table, entity, revColumn, columns.toList())

fun <E, T : Table<E>> Database.updateOptimistic(
    table: T,
    entity: E,
    revColumn: Column<E, Int> = table.revColumn,
    columnsSelector: ColumnSelectionBuilder<E>.(T) -> Unit,
) = updateOptimistic(table, entity, revColumn, table.selectColumns(columnsSelector))

// endregion

// region 批量更新实体

// 独立出个方法解决泛型问题（方法内无法推断C的类型）
private fun <E, C : Any> Column<E, C>.caseValueExpr(
    primaryKeys: List<Column<E, *>>,
    entities: List<E>,
) = CaseValueExpression<Nothing, C>(
    Tuple(primaryKeys), entities.map { entity ->
        Tuple(primaryKeys.map { it.getValueExpr(entity) }) to getValueExpr(entity)
    }
)

fun <E, T : Table<E>> Database.update(
    table: T,
    entities: List<E>,
    columns: List<Column<E, *>> = table.updateColumns,
) = update(table) {
    val primaryKeys = it.primaryKeys
    columns.forEach { set(it) { it.caseValueExpr(primaryKeys, entities) } }
    where { it.primaryKeyFilter(entities) }
}

fun <E, T : Table<E>> Database.update(
    table: T,
    entities: List<E>,
    vararg columns: Column<E, *>,
) = update(table, entities, columns.toList())

fun <E, T : Table<E>> Database.update(
    table: T,
    entities: List<E>,
    columnsSelector: ColumnSelectionBuilder<E>.(T) -> Unit,
) = update(table, entities, table.selectColumns(columnsSelector))

// endregion

// region 逻辑删除实体

fun <E, T : Table<E>> Database.deleteLogically(
    table: T,
    entity: E,
    deletedColumn: Column<E, Boolean> = table.deletedColumn,
) = update(table) {
    set(deletedColumn, true)
    where { it.primaryKeyFilter(entity) }
}

fun <E, T : Table<E>> Database.deleteLogically(
    table: T,
    entity: E,
    selectDeletedColumn: (T) -> Column<E, Boolean>,
) = deleteLogically(table, entity, selectDeletedColumn(table))

// endregion

// region 批量逻辑删除实体

fun <E, T : Table<E>> Database.deleteLogically(
    table: T,
    entities: List<E>,
    deletedColumn: Column<E, Boolean> = table.deletedColumn,
) = update(table) {
    set(deletedColumn, true)
    where { it.primaryKeyFilter(entities) }
}

fun <E, T : Table<E>> Database.deleteLogically(
    table: T,
    entities: List<E>,
    selectDeletedColumn: (T) -> Column<E, Boolean>,
) = deleteLogically(table, entities, selectDeletedColumn(table))

// endregion
