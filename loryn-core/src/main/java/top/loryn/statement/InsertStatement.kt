package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.database.mapEachRow
import top.loryn.expression.ColumnExpression
import top.loryn.expression.ParameterExpression
import top.loryn.expression.SelectExpression
import top.loryn.expression.expr
import top.loryn.schema.Column
import top.loryn.schema.Table
import java.sql.ResultSet

data class InsertStatement(
    val database: Database, val table: Table<*>,
    val columns: List<ColumnExpression<*>>,
    val values: List<ParameterExpression<*>>? = null,
    val select: SelectExpression<*>? = null,
    val useGeneratedKeys: Boolean = false,
) : Statement() {
    init {
        require(columns.isNotEmpty()) { "At least one column must be set" }
        if (values == null && select == null) {
            throw IllegalArgumentException("Either values or select must be set")
        }
        if (values != null && select != null) {
            throw IllegalArgumentException("Only one of values and select can be set")
        }
        if (values != null && values.size != columns.size) {
            throw IllegalArgumentException("The number of values must match the number of columns")
        }
    }

    override fun generateSql() = database.buildSql { params ->
        appendKeyword("INSERT").append(' ').appendKeyword("INTO").append(' ').appendTable(table).append(" (")
        columns.forEachIndexed { index, column ->
            if (index > 0) append(", ")
            appendExpression(column, params)
        }
        append(") ")
        if (values != null) {
            appendKeyword("VALUES").append(" (")
            values.forEachIndexed { index, value ->
                if (index > 0) append(", ")
                appendExpression(value, params)
            }
            append(')')
        } else {
            appendExpression(select!!, params)
        }
    }

    fun execute(forEachGeneratedKey: (ResultSet) -> Unit = {}) =
        database.doExecute(useGeneratedKeys) { statement ->
            statement.executeUpdate().also(database::showEffects).also {
                if (useGeneratedKeys) {
                    statement.generatedKeys.mapEachRow(forEachGeneratedKey)
                }
            }
        }
}

@LorynDsl
class InsertBuilder<T : Table<*>>(
    table: T, val useGeneratedKeys: Boolean = false,
) : StatementBuilder<T, InsertStatement>(table) {
    internal val columns = mutableListOf<ColumnExpression<*>>()
    internal val values = mutableListOf<ParameterExpression<*>>()
    internal var select: SelectExpression<*>? = null

    fun <C : Any> column(column: Column<C>) {
        columns += column.also(::checkColumn)
    }

    fun columns(columns: List<Column<*>>) {
        this.columns += columns.onEach(::checkColumn)
    }

    fun columns(vararg columns: Column<*>) {
        columns(columns.toList())
    }

    private fun requireNullSelect() {
        require(select == null) { "Cannot set both values and select" }
    }

    fun value(value: ParameterExpression<*>) {
        requireNullSelect()
        this.values += value
    }

    fun values(values: List<ParameterExpression<*>>) {
        requireNullSelect()
        this.values += values
    }

    fun values(vararg values: ParameterExpression<*>) {
        values(values.toList())
    }

    fun <T : Any> assign(column: Column<T>, value: T?) {
        column(column)
        value(column.expr(value))
    }

    private fun requireEmptyValues() {
        require(values.isEmpty()) { "Cannot set both values and select" }
    }

    fun select(select: SelectExpression<*>) {
        requireEmptyValues()
        this.select = select
    }

    override fun build(database: Database) =
        InsertStatement(database, table, columns, values.takeUnless { it.isEmpty() }, select, useGeneratedKeys)
}

fun <T : Table<*>> Database.insert(table: T, useGeneratedKeys: Boolean = false, block: InsertBuilder<T>.(T) -> Unit) =
    InsertBuilder(table, useGeneratedKeys).apply { block(table) }.build(this)
