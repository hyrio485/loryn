package top.loryn.dialect.mysql

import top.loryn.database.Database
import top.loryn.expression.ColumnExpression
import top.loryn.expression.ParameterExpression
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.statement.BaseInsertStatement
import top.loryn.utils.checkTableColumn

class BatchInsertStatement<E>(
    database: Database,
    table: Table<E>,
    columns: List<ColumnExpression<E, *>>,
    val values: List<List<ParameterExpression<E, *>>>,
    useGeneratedKeys: Boolean = false,
) : BaseInsertStatement<E>(database, table, columns, useGeneratedKeys) {
    init {
        val columnCount = columns.size
        require(columnCount > 0) { "At least one column must be set" }
        require(values.isNotEmpty()) { "At least one row must be set" }
        values.forEachIndexed { index, values ->
            require(values.size == columnCount) { "The number of columns of row with index $index must match the number of columns: $columnCount" }
        }
    }

    override fun generateSql() = database.buildSql { params ->
        appendInsertIntoColumns(params).appendKeyword("VALUES").append(' ')
        appendList(values, params) { values, params ->
            appendRowValues(values, params)
        }
    }
}

fun <E, T : Table<E>> Database.batchInsert(
    table: T,
    entities: List<E>,
    columns: List<Column<E, *>> = table.insertColumns,
    useGeneratedKeys: Boolean = false,
): Int {
    val columns =
        columns.takeIf { it.isNotEmpty() }?.onEach { checkTableColumn(table, it) } ?: table.columns
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

fun <E, T : Table<E>> Database.batchInsert(
    table: T,
    entities: List<E>,
    vararg columns: Column<E, *>,
    useGeneratedKeys: Boolean = false,
) = batchInsert(table, entities, columns.toList(), useGeneratedKeys)
