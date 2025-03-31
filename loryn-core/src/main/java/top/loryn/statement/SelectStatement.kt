package top.loryn.statement

import top.loryn.database.Database
import top.loryn.expression.SelectExpression
import top.loryn.schema.Table

class SelectStatement<E>(
    database: Database,
    val select: SelectExpression<E>,
) : DqlStatement<E>(database) {
    override val createEntity = select::createEntity
    override val columns = select.columns.takeIf { it.isNotEmpty() } ?: select.from?.columns

    override fun generateSql() = database.buildSql { params ->
        appendExpression(select, params)
    }

    fun count() = database.doExecute(getSqlAndParams = {
        database.buildSql { params ->
            select.run { appendSqlCount(params) }
        }
    }) { statement ->
        statement.executeQuery().use { resultSet ->
            if (!resultSet.next()) {
                error("No result found")
            }
            resultSet.getInt(1)
        }
    }
}

fun <E, T : Table<E>> T.select(
    block: SelectExpression.Builder<E, T>.(T) -> Unit = {},
) = SelectExpression.Builder(this).apply { block(table) }.build()

fun <E, T : Table<E>> Database.select(table: T, block: SelectExpression.Builder<E, T>.(T) -> Unit = {}) =
    SelectStatement(this, table.select(block))
