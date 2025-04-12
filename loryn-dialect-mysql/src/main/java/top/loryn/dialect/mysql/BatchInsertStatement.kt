package top.loryn.dialect.mysql

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlParam
import top.loryn.schema.BindableColumn
import top.loryn.schema.BindableTable
import top.loryn.schema.Table
import top.loryn.statement.BaseInsertStatement
import top.loryn.statement.ColumnSelectionBuilder
import top.loryn.statement.selectColumns
import top.loryn.utils.SqlParamList

class BatchInsertStatement(
    database: Database,
    table: Table,
    columns: List<ColumnExpression<*>>,
    val values: List<List<SqlParam<*>>>,
    useGeneratedKeys: Boolean = false,
) : BaseInsertStatement(database, table, columns, useGeneratedKeys) {
    init {
        val columnCount = columns.size
        require(columnCount > 0) { "At least one column must be set" }
        require(values.isNotEmpty()) { "At least one row must be set" }
        values.forEachIndexed { index, values ->
            require(values.size == columnCount) { "The number of columns of row with index $index must match the number of columns: $columnCount" }
        }
    }

    override fun SqlBuilder.doGenerateSql(params: SqlParamList) = also {
        appendInsertIntoColumns(params).appendKeyword("VALUES").append(' ')
        appendList(values, params) { values, params ->
            appendRowValues(values, params)
        }
    }
}

fun <E, T : BindableTable<E>> Database.batchInsert(
    table: T,
    entities: List<E>,
    useGeneratedKeys: Boolean = false,
    columns: List<BindableColumn<E, *>> = table.insertColumns,
): Int {
    val columns = columns
    return BatchInsertStatement(
        this, table, columns,
        entities.map { entity -> columns.map { it.getValueExpr(entity) } },
        useGeneratedKeys = useGeneratedKeys,
    ).let { statement ->
        var currentIndex = 0
        statement.execute { rs ->
            val primaryKeys = table.primaryKeys
            if (primaryKeys.isEmpty()) {
                error("No primary keys found for table $table")
            }
            primaryKeys.forEachIndexed { index, column ->
                column.setValue(entities[currentIndex++], index, rs)
            }
        }
    }
}

fun <E, T : BindableTable<E>> Database.batchInsert(
    table: T,
    entities: List<E>,
    useGeneratedKeys: Boolean = false,
    vararg columns: BindableColumn<E, *>,
) = batchInsert(table, entities, useGeneratedKeys, columns.toList())

fun <E, T : BindableTable<E>> Database.batchInsert(
    table: T,
    entities: List<E>,
    useGeneratedKeys: Boolean = false,
    columnsSelector: ColumnSelectionBuilder<BindableColumn<E, *>>.(T) -> Unit,
) = batchInsert(table, entities, useGeneratedKeys, table.selectColumns(columnsSelector))
