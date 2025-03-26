package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.expression.ColumnExpression
import top.loryn.expression.ParameterExpression
import top.loryn.expression.SelectExpression
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.schema.checkTableColumn

class InsertStatement<E>(
    database: Database,
    val table: Table<*>,
    val columns: List<ColumnExpression<E, *>>,
    val values: List<ParameterExpression<E, *>>? = null,
    val select: SelectExpression<*>? = null,
    useGeneratedKeys: Boolean = false,
) : DmlStatement(database, useGeneratedKeys) {
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
}

@LorynDsl
class InsertBuilder<E, T : Table<E>>(
    table: T, val useGeneratedKeys: Boolean = false,
) : StatementBuilder<T, InsertStatement<E>>(table) {
    internal val columns = mutableListOf<ColumnExpression<E, *>>()
    internal val values = mutableListOf<ParameterExpression<E, *>>()
    internal var select: SelectExpression<*>? = null

    fun <C : Any> column(column: Column<E, C>) {
        columns += column.also { checkTableColumn(table, it) }
    }

    fun columns(columns: List<Column<E, *>>) {
        this.columns += columns.onEach { checkTableColumn(table, it) }
    }

    fun columns(vararg columns: Column<E, *>) {
        columns(columns.toList())
    }

    private fun requireNullSelect() {
        require(select == null) { "Cannot set both values and select" }
    }

    fun value(value: ParameterExpression<E, *>) {
        requireNullSelect()
        this.values += value
    }

    fun values(values: List<ParameterExpression<E, *>>) {
        requireNullSelect()
        this.values += values
    }

    fun values(vararg values: ParameterExpression<E, *>) {
        values(values.toList())
    }

    fun <C : Any> assign(column: Column<E, C>, value: C?) {
        if (column.notNull && value == null) {
            throw IllegalArgumentException("Column ${column.name} cannot be null")
        }
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

fun <E, T : Table<E>> Database.insert(
    table: T,
    useGeneratedKeys: Boolean = false,
    block: InsertBuilder<E, T>.(T) -> Unit = {},
) = InsertBuilder(table, useGeneratedKeys).apply { block(table) }.build(this)

fun <E, T : Table<E>> Database.insert(
    table: T,
    entity: E,
    columns: List<Column<E, *>> = table.insertColumns,
    useGeneratedKeys: Boolean = false,
): Int {
    val columns =
        columns.takeIf { it.isNotEmpty() }?.onEach { checkTableColumn(table, it) } ?: table.columns
    return InsertStatement(
        this, table, columns, columns.map { it.getValueExpr(entity) }, useGeneratedKeys = useGeneratedKeys,
    ).execute { rs ->
        val primaryKeys = table.primaryKeys
        if (primaryKeys.isEmpty()) {
            error("No primary keys found for table $table")
        }
        primaryKeys.forEachIndexed { index, column ->
            column.setValue(entity, index, rs)
        }
    }
}

fun <E, T : Table<E>> Database.insert(
    table: T,
    entity: E,
    vararg columns: Column<E, *>,
    useGeneratedKeys: Boolean = false,
) = insert(table, entity, columns.toList(), useGeneratedKeys)
