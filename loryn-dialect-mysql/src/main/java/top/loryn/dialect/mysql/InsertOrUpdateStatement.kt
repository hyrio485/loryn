package top.loryn.dialect.mysql

import top.loryn.database.Database
import top.loryn.expression.AssignmentExpression
import top.loryn.expression.ColumnExpression
import top.loryn.expression.ParameterExpression
import top.loryn.expression.SqlExpression
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.statement.BaseInsertStatement
import top.loryn.statement.StatementBuilder
import top.loryn.support.LorynDsl
import top.loryn.utils.checkTableColumn

class InsertOrUpdateStatement<E>(
    database: Database,
    table: Table<E>,
    columns: List<ColumnExpression<E, *>>,
    val values: List<ParameterExpression<E, *>>,
    val sets: List<AssignmentExpression<E, *>>,
    useGeneratedKeys: Boolean = false,
) : BaseInsertStatement<E>(database, table, columns, useGeneratedKeys) {
    init {
        require(columns.isNotEmpty()) { "At least one column must be set" }
        if (values.size != columns.size) {
            throw IllegalArgumentException("The number of values must match the number of columns")
        }
        require(sets.isNotEmpty()) { "At least one column for update must be set" }
    }

    override fun generateSql() = database.buildSql { params ->
        appendInsertIntoColumns(params).appendKeyword("VALUES")
        append(' ').appendRowValues(values, params)
        append(' ').appendKeyword("ON")
        append(' ').appendKeyword("DUPLICATE")
        append(' ').appendKeyword("KEY")
        append(' ').appendKeyword("UPDATE")
        append(' ').appendExpressions(sets, params)
    }
}

@LorynDsl
class InsertOrUpdateBuilder<E, T : Table<E>>(
    table: T, val useGeneratedKeys: Boolean = false,
) : StatementBuilder<T, InsertOrUpdateStatement<E>>(table) {
    private val columns = mutableListOf<ColumnExpression<E, *>>()
    private val values = mutableListOf<ParameterExpression<E, *>>()
    private val sets = mutableListOf<AssignmentExpression<E, *>>()

    fun <C : Any> assign(column: Column<E, C>, value: C?) {
        if (column.notNull && value == null) {
            throw IllegalArgumentException("Column ${column.name} cannot be null")
        }
        columns += column.also { checkTableColumn(table, it) }
        values += column.expr(value)
    }

    fun <C : Any> set(column: Column<E, C>, value: SqlExpression<C>) {
        checkTableColumn(table, column)
        require(column in columns) { "Column $column is not in the insert columns" }
        sets += AssignmentExpression(column, value)
    }

    fun <C : Any> set(column: Column<E, C>, block: (Column<E, C>) -> SqlExpression<C>) {
        set(column, block(column))
    }

    fun <C : Any> set(column: Column<E, C>, value: C?) {
        set(column, column.expr(value))
    }

    override fun buildStatement(database: Database) =
        InsertOrUpdateStatement(database, table, columns, values, sets, useGeneratedKeys)
}

fun <E, T : Table<E>> Database.insertOrUpdate(
    table: T,
    useGeneratedKeys: Boolean = false,
    block: InsertOrUpdateBuilder<E, T>.(T) -> Unit = {},
) = InsertOrUpdateBuilder(table, useGeneratedKeys).apply { block(table) }.buildStatement(this)

fun <E, T : Table<E>> Database.insertOrUpdate(
    table: T,
    entity: E,
    columns: List<Column<E, *>> = table.insertColumns,
    sets: List<AssignmentExpression<E, *>>,
    useGeneratedKeys: Boolean = false,
): Int {
    val columns =
        columns.takeIf { it.isNotEmpty() }?.onEach { checkTableColumn(table, it) } ?: table.columns
    return InsertOrUpdateStatement(
        this, table, columns, columns.map { it.getValueExpr(entity) }, sets, useGeneratedKeys = useGeneratedKeys,
    ).let { it.execute { rs -> it.fillInPrimaryKeysForEachRow(entity, rs) } }
}

fun <E, T : Table<E>> Database.insertOrUpdate(
    table: T,
    entity: E,
    vararg columns: Column<E, *>,
    sets: List<AssignmentExpression<E, *>>,
    useGeneratedKeys: Boolean = false,
) = insertOrUpdate(table, entity, columns.toList(), sets, useGeneratedKeys)
