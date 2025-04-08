package top.loryn.statement

import top.loryn.database.Database
import top.loryn.expression.QuerySourceExpression
import top.loryn.expression.SelectExpression

class SelectStatement<E>(
    database: Database,
    val select: SelectExpression<E>,
) : DqlStatement<E>(database) {
    override val createEntity = select::createEntity
    override val columns = select.columns.takeIf { it.isNotEmpty() } ?: select.from?.columns
    override val usingIndex = select.columns.isNotEmpty()

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

fun <E, T : QuerySourceExpression<E>> T.select(
    block: SelectExpression.Builder<E, T>.(T) -> Unit = {},
) = SelectExpression.Builder(this).apply { block(this@select) }.build()

fun <E, T : QuerySourceExpression<E>> Database.select(
    querySource: T,
    block: SelectExpression.Builder<E, T>.(T) -> Unit = {},
) = SelectStatement(this, querySource.select(block))
