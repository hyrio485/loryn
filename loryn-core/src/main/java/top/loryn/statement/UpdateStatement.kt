package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.*
import top.loryn.schema.BindableColumn
import top.loryn.schema.BindableTable
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.support.LorynDsl
import top.loryn.support.Tuple
import top.loryn.utils.SqlParamList

class UpdateStatement(
    override val database: Database,
    val table: Table,
    val sets: List<AssignmentExpression<*>>,
    val where: SqlExpression<Boolean>?,
) : DmlStatement {
    init {
        require(sets.isNotEmpty()) { "At least one column must be set" }
    }

    override fun SqlBuilder.doGenerateSql(params: SqlParamList) = also {
        appendKeyword("UPDATE").append(' ').append(table).append(' ')
        appendKeyword("SET").append(' ').append(sets, params)
        where?.also { append(' ').appendKeyword("WHERE").append(' ').append(it, params) }
    }
}

@LorynDsl
class UpdateBuilder<T : Table>(table: T) : StatementBuilder<T, UpdateStatement>(table) {
    private val sets = mutableListOf<AssignmentExpression<*>>()
    private var where: SqlExpression<Boolean>? = null

    fun <C> set(column: Column<C>, value: SqlExpression<C>) {
        sets += AssignmentExpression(column, value)
    }

    fun <C> set(column: Column<C>, block: (Column<C>) -> SqlExpression<C>) {
        set(column, block(column))
    }

    fun <C> set(column: Column<C>, value: C?) {
        set(column, column.expr(value))
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun buildStatement(database: Database) = UpdateStatement(database, table, sets, where)
}

fun <T : Table> Database.update(
    table: T,
    block: UpdateBuilder<T>.(T) -> Unit = {},
) = UpdateBuilder(table).apply { block(table) }.buildStatement(this).execute()

@LorynDsl
class BindableUpdateBuilder<E, T : BindableTable<E>>(table: T) : StatementBuilder<T, UpdateStatement>(table) {
    private val sets = mutableListOf<AssignmentExpression<*>>()
    private var where: SqlExpression<Boolean>? = null

    fun <C> set(column: BindableColumn<E, C>, value: SqlExpression<C>) {
        sets += AssignmentExpression(column, value)
    }

    fun <C> set(column: BindableColumn<E, C>, block: (BindableColumn<E, C>) -> SqlExpression<C>) {
        set(column, block(column))
    }

    fun <C> set(column: BindableColumn<E, C>, value: C?) {
        set(column, column.expr(value))
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun buildStatement(database: Database) = UpdateStatement(database, table, sets, where)
}

fun <E, T : BindableTable<E>> Database.updateBindable(
    table: T,
    block: BindableUpdateBuilder<E, T>.(T) -> Unit = {},
) = BindableUpdateBuilder(table).apply { block(table) }.buildStatement(this).execute()

// region 更新实体

fun <E, T : BindableTable<E>> Database.update(
    table: T,
    entity: E,
    columns: List<BindableColumn<E, *>> = table.updateColumns,
) = updateBindable(table) {
    columns.forEach { set(it) { it.getValueExpr(entity) } }
    where { it.primaryKeyFilter(entity) }
}

fun <E, T : BindableTable<E>> Database.update(
    table: T,
    entity: E,
    vararg columns: BindableColumn<E, *>,
) = update(table, entity, columns.toList())

fun <E, T : BindableTable<E>> Database.update(
    table: T,
    entity: E,
    columnsSelector: ColumnSelectionBuilder<BindableColumn<E, *>>.(T) -> Unit,
) = update(table, entity, table.selectColumns(columnsSelector))

// endregion

// region 乐观锁更新实体

fun <E, T : BindableTable<E>> Database.updateOptimistic(
    table: T,
    entity: E,
    revColumn: BindableColumn<E, Int> = table.revColumn,
    columns: List<BindableColumn<E, *>> = table.updateColumns,
): Int {
    require(revColumn !in columns) { "Revision column $revColumn cannot be included in the update columns" }
    return updateBindable(table) {
        columns.forEach { column ->
            set(column) { it.getValueExpr(entity) }
            set(revColumn, revColumn.getValue(entity)!! + 1)
        }
        where { it.primaryKeyFilter(entity) and (revColumn eq revColumn.getValueExpr(entity)) }
    }
}

fun <E, T : BindableTable<E>> Database.updateOptimistic(
    table: T,
    entity: E,
    revColumn: BindableColumn<E, Int> = table.revColumn,
    vararg columns: BindableColumn<E, *>,
) = updateOptimistic(table, entity, revColumn, columns.toList())

fun <E, T : BindableTable<E>> Database.updateOptimistic(
    table: T,
    entity: E,
    revColumn: BindableColumn<E, Int> = table.revColumn,
    columnsSelector: ColumnSelectionBuilder<BindableColumn<E, *>>.(T) -> Unit,
) = updateOptimistic(table, entity, revColumn, table.selectColumns(columnsSelector))

// endregion

// region 批量更新实体

// 独立出个方法解决泛型问题（方法内无法推断C的类型）
private fun <E, T> BindableColumn<E, T>.caseValueExpr(
    primaryKeys: List<BindableColumn<E, *>>,
    entities: List<E>,
) = CaseValueExpression<Nothing, T>(
    Tuple(primaryKeys), entities.map { entity ->
        Tuple(primaryKeys.map { it.getValueExpr(entity) }) to getValueExpr(entity)
    }
)

fun <E, T : BindableTable<E>> Database.update(
    table: T,
    entities: List<E>,
    columns: List<BindableColumn<E, *>> = table.updateColumns,
) = updateBindable(table) {
    val primaryKeys = it.primaryKeys
    columns.forEach { set(it) { it.caseValueExpr(primaryKeys, entities) } }
    where { it.primaryKeyFilter(entities) }
}

fun <E, T : BindableTable<E>> Database.update(
    table: T,
    entities: List<E>,
    vararg columns: BindableColumn<E, *>,
) = update(table, entities, columns.toList())

fun <E, T : BindableTable<E>> Database.update(
    table: T,
    entities: List<E>,
    columnsSelector: ColumnSelectionBuilder<BindableColumn<E, *>>.(T) -> Unit,
) = update(table, entities, table.selectColumns(columnsSelector))

// endregion

// region 逻辑删除实体

fun <E, T : BindableTable<E>> Database.deleteLogically(
    table: T,
    entity: E,
    deletedColumn: BindableColumn<E, Boolean> = table.deletedColumn,
) = update(table) {
    set(deletedColumn, true)
    where { it.primaryKeyFilter(entity) }
}

fun <E, T : BindableTable<E>> Database.deleteLogically(
    table: T,
    entity: E,
    selectDeletedColumn: (T) -> BindableColumn<E, Boolean>,
) = deleteLogically(table, entity, selectDeletedColumn(table))

// endregion

// region 批量逻辑删除实体

fun <E, T : BindableTable<E>> Database.deleteLogically(
    table: T,
    entities: List<E>,
    deletedColumn: BindableColumn<E, Boolean> = table.deletedColumn,
) = update(table) {
    set(deletedColumn, true)
    where { it.primaryKeyFilter(entities) }
}

fun <E, T : BindableTable<E>> Database.deleteLogically(
    table: T,
    entities: List<E>,
    selectDeletedColumn: (T) -> BindableColumn<E, Boolean>,
) = deleteLogically(table, entities, selectDeletedColumn(table))

// endregion
