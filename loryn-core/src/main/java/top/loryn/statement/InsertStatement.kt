package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.ParameterExpression
import top.loryn.expression.SelectExpression
import top.loryn.expression.SqlParam
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.support.LorynDsl
import top.loryn.utils.checkTableColumn
import java.sql.ResultSet

abstract class BaseInsertStatement<E>(
    database: Database,
    val table: Table<E>,
    val columns: List<ColumnExpression<E, *>>,
    useGeneratedKeys: Boolean,
) : DmlStatement(database, useGeneratedKeys) {
    protected fun SqlBuilder.appendInsertIntoColumns(params: MutableList<SqlParam<*>>) = also {
        appendKeyword("INSERT").append(' ').appendKeyword("INTO").append(' ')
        appendTable(table).append(" (").appendExpressions(columns, params).append(") ")
    }

    protected fun SqlBuilder.appendRowValues(
        values: List<ParameterExpression<*>>,
        params: MutableList<SqlParam<*>>,
    ) = also { append('(').appendExpressions(values, params).append(')') }

    fun fillInPrimaryKeysForEachRow(entity: E, rs: ResultSet) {
        val primaryKeys = table.primaryKeys
        if (primaryKeys.isEmpty()) {
            error("No primary keys found for table $table")
        }
        primaryKeys.forEachIndexed { index, column ->
            column.setValue(entity, index, rs)
        }
    }
}

class InsertStatement<E>(
    database: Database,
    table: Table<E>,
    columns: List<ColumnExpression<E, *>>,
    val values: List<ParameterExpression<*>>? = null,
    val select: SelectExpression<*>? = null,
    useGeneratedKeys: Boolean = false,
) : BaseInsertStatement<E>(database, table, columns, useGeneratedKeys) {
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
        appendInsertIntoColumns(params)
        if (values != null) {
            appendKeyword("VALUES").append(' ').appendRowValues(values, params)
        } else {
            appendExpression(select!!, params)
        }
    }
}

@LorynDsl
class InsertBuilder<E, T : Table<E>>(
    table: T, private val useGeneratedKeys: Boolean = false,
) : StatementBuilder<T, InsertStatement<E>>(table) {
    private val columns = mutableListOf<ColumnExpression<E, *>>()
    private val values = mutableListOf<ParameterExpression<*>>()
    private var select: SelectExpression<*>? = null

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

    override fun buildStatement(database: Database) =
        InsertStatement(database, table, columns, values.takeUnless { it.isEmpty() }, select, useGeneratedKeys)
}

fun <E, T : Table<E>> Database.insert(
    table: T,
    useGeneratedKeys: Boolean = false,
    block: InsertBuilder<E, T>.(T) -> Unit = {},
) = InsertBuilder(table, useGeneratedKeys).apply { block(table) }.buildStatement(this)

// region 插入实体

fun <E, T : Table<E>> Database.insert(
    table: T,
    entity: E,
    useGeneratedKeys: Boolean = false,
    columns: List<Column<E, *>> = table.insertColumns,
): Int {
    val columns = columns.onEach { checkTableColumn(table, it) }
    return InsertStatement(
        this, table, columns, columns.map { it.getValueExpr(entity) }, useGeneratedKeys = useGeneratedKeys,
    ).let { it.execute { rs -> it.fillInPrimaryKeysForEachRow(entity, rs) } }
}

fun <E, T : Table<E>> Database.insert(
    table: T,
    entity: E,
    useGeneratedKeys: Boolean = false,
    vararg columns: Column<E, *>,
) = insert(table, entity, useGeneratedKeys, columns.toList())

fun <E, T : Table<E>> Database.insert(
    table: T,
    entity: E,
    useGeneratedKeys: Boolean = false,
    columnsSelector: ColumnSelectionBuilder<E>.(T) -> Unit,
) = insert(table, entity, useGeneratedKeys, table.selectColumns(columnsSelector))

// endregion
