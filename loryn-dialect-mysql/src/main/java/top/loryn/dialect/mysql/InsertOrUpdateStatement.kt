package top.loryn.dialect.mysql

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.AssignmentExpression
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlExpression
import top.loryn.expression.SqlParam
import top.loryn.schema.BindableColumn
import top.loryn.schema.BindableTable
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.statement.BaseInsertStatement
import top.loryn.statement.ColumnSelectionBuilder
import top.loryn.statement.StatementBuilder
import top.loryn.statement.selectColumns
import top.loryn.support.LorynDsl
import top.loryn.utils.SqlParamList

class InsertOrUpdateStatement(
    database: Database,
    table: Table,
    columns: List<ColumnExpression<*>>,
    val values: List<SqlParam<*>>,
    val sets: List<AssignmentExpression<*>>,
    useGeneratedKeys: Boolean = false,
) : BaseInsertStatement(database, table, columns, useGeneratedKeys) {
    init {
        require(columns.isNotEmpty()) { "At least one column must be set" }
        if (values.size != columns.size) {
            throw IllegalArgumentException("The number of values must match the number of columns")
        }
        require(sets.isNotEmpty()) { "At least one column for update must be set" }
    }

    override fun SqlBuilder.doGenerateSql(params: SqlParamList) = also {
        appendInsertIntoColumns(params).appendKeyword("VALUES")
        append(' ').appendRowValues(values, params)
        append(' ').appendKeyword("ON")
        append(' ').appendKeyword("DUPLICATE")
        append(' ').appendKeyword("KEY")
        append(' ').appendKeyword("UPDATE")
        append(' ').append(sets, params)
    }
}

@LorynDsl
class InsertOrUpdateBuilder<T : Table>(
    table: T, val useGeneratedKeys: Boolean = false,
) : StatementBuilder<T, InsertOrUpdateStatement>(table) {
    private val columns = mutableListOf<ColumnExpression<*>>()
    private val values = mutableListOf<SqlParam<*>>()
    private val sets = mutableListOf<AssignmentExpression<*>>()

    fun <C> assign(column: Column<C>, value: C?) {
        if (column.notNull && value == null) {
            throw IllegalArgumentException("Column ${column.name} cannot be null")
        }
        columns += column
        values += column.expr(value)
    }

    fun <C> set(column: Column<C>, value: SqlExpression<C>) {
        require(column in columns) { "Column $column is not in the insert columns" }
        sets += AssignmentExpression(column, value)
    }

    fun <C> set(column: Column<C>, block: (Column<C>) -> SqlExpression<C>) {
        set(column, block(column))
    }

    fun <C> set(column: Column<C>, value: C?) {
        set(column, column.expr(value))
    }

    override fun buildStatement(database: Database) =
        InsertOrUpdateStatement(database, table, columns, values, sets, useGeneratedKeys)
}

fun <T : Table> Database.insertOrUpdate(
    table: T,
    useGeneratedKeys: Boolean = false,
    block: InsertOrUpdateBuilder<T>.(T) -> Unit = {},
) = InsertOrUpdateBuilder(table, useGeneratedKeys).apply { block(table) }.buildStatement(this)

// region 插入或更新实体

fun <E, T : BindableTable<E>> Database.insertOrUpdate(
    table: T,
    entity: E,
    useGeneratedKeys: Boolean = false,
    columns: List<BindableColumn<E, *>> = table.insertColumns,
    updateColumns: List<BindableColumn<E, *>> = table.updateColumns,
): Int {
    val columns = columns
    return InsertOrUpdateStatement(
        this, table, columns,
        columns.map { it.getValueExpr(entity) },
        updateColumns.map { it.assignByEntity(entity) },
        useGeneratedKeys = useGeneratedKeys,
    ).execute { BaseInsertStatement.fillInPrimaryKeysForEachRow(table, entity, it) }
}

fun <E, T : BindableTable<E>> Database.insertOrUpdate(
    table: T,
    entity: E,
    useGeneratedKeys: Boolean = false,
    columnsSelector: ColumnSelectionBuilder<BindableColumn<E, *>>.(T) -> Unit,
    updateColumnsSelector: ColumnSelectionBuilder<BindableColumn<E, *>>.(T) -> Unit,
) = insertOrUpdate(
    table,
    entity,
    useGeneratedKeys,
    table.selectColumns(columnsSelector),
    table.selectColumns(updateColumnsSelector),
)

// endregion
