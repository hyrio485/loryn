package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SelectExpression
import top.loryn.expression.SqlParam
import top.loryn.schema.BindableColumn
import top.loryn.schema.BindableTable
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.support.LorynDsl
import top.loryn.utils.SqlParamList
import java.sql.ResultSet

abstract class BaseInsertStatement(
    override val database: Database,
    val table: Table,
    val columns: List<ColumnExpression<*>>,
    override val useGeneratedKeys: Boolean,
) : DmlStatement {
    protected fun SqlBuilder.appendInsertIntoColumns(params: SqlParamList) = also {
        appendKeyword("INSERT").append(' ').appendKeyword("INTO").append(' ')
        append(table).append(" (").append(columns, params).append(") ")
    }

    protected fun SqlBuilder.appendRowValues(values: List<SqlParam<*>>, params: SqlParamList) =
        append('(').append(values, params).append(')')

    companion object {
        fun <E> fillInPrimaryKeysForEachRow(table: BindableTable<E>, entity: E, rs: ResultSet) {
            val primaryKeys = table.primaryKeys
            if (primaryKeys.isEmpty()) {
                error("No primary keys found for table $table")
            }
            primaryKeys.forEachIndexed { index, column ->
                column.setValue(entity, index, rs)
            }
        }
    }
}

class InsertStatement(
    database: Database,
    table: Table,
    columns: List<ColumnExpression<*>>,
    val values: List<SqlParam<*>>? = null,
    val select: SelectExpression? = null,
    useGeneratedKeys: Boolean = false,
) : BaseInsertStatement(database, table, columns, useGeneratedKeys) {
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

    override fun SqlBuilder.doGenerateSql(params: SqlParamList) = also {
        appendInsertIntoColumns(params)
        if (values != null) {
            appendKeyword("VALUES").append(' ').appendRowValues(values, params)
        } else {
            append(select!!, params)
        }
    }
}

@LorynDsl
class InsertBuilder<T : Table>(
    table: T, private val useGeneratedKeys: Boolean = false,
) : StatementBuilder<T, InsertStatement>(table) {
    private val columns = mutableListOf<ColumnExpression<*>>()
    private val values = mutableListOf<SqlParam<*>>()
    private var select: SelectExpression? = null

    fun <C> column(column: Column<C>) {
        columns += column
    }

    fun columns(columns: List<Column<*>>) {
        this.columns += columns
    }

    fun columns(vararg columns: Column<*>) {
        columns(columns.toList())
    }

    private fun requireNullSelect() {
        require(select == null) { "Cannot set both values and select" }
    }

    fun value(value: SqlParam<*>) {
        requireNullSelect()
        this.values += value
    }

    fun values(values: List<SqlParam<*>>) {
        requireNullSelect()
        this.values += values
    }

    fun values(vararg values: SqlParam<*>) {
        values(values.toList())
    }

    fun <C> assign(column: Column<C>, value: C?) {
        if (column.notNull && value == null) {
            throw IllegalArgumentException("Column ${column.name} cannot be null")
        }
        column(column)
        value(column.expr(value))
    }

    private fun requireEmptyValues() {
        require(values.isEmpty()) { "Cannot set both values and select" }
    }

    fun select(select: SelectExpression) {
        requireEmptyValues()
        this.select = select
    }

    override fun buildStatement(database: Database) =
        InsertStatement(database, table, columns, values.takeUnless { it.isEmpty() }, select, useGeneratedKeys)
}

fun <T : Table> Database.insert(
    table: T,
    useGeneratedKeys: Boolean = false,
    block: InsertBuilder<T>.(T) -> Unit = {},
) = InsertBuilder(table, useGeneratedKeys).apply { block(table) }.buildStatement(this)

// region 插入实体

fun <E, T : BindableTable<E>> Database.insert(
    table: T,
    entity: E,
    useGeneratedKeys: Boolean = false,
    columns: List<BindableColumn<E, *>> = table.insertColumns,
) = columns.let { columns ->
    InsertStatement(
        this, table, columns, columns.map { it.getValueExpr(entity) }, useGeneratedKeys = useGeneratedKeys,
    ).execute { BaseInsertStatement.fillInPrimaryKeysForEachRow(table, entity, it) }
}

fun <E, T : BindableTable<E>> Database.insert(
    table: T,
    entity: E,
    useGeneratedKeys: Boolean = false,
    vararg columns: BindableColumn<E, *>,
) = insert(table, entity, useGeneratedKeys, columns.toList())

fun <E, T : BindableTable<E>> Database.insert(
    table: T,
    entity: E,
    useGeneratedKeys: Boolean = false,
    columnsSelector: ColumnSelectionBuilder<BindableColumn<E, *>>.(T) -> Unit,
) = insert(table, entity, useGeneratedKeys, table.selectColumns(columnsSelector))

// endregion
