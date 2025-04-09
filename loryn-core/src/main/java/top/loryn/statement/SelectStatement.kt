package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.QuerySourceExpression
import top.loryn.expression.SelectExpression
import top.loryn.expression.SqlParam

class SelectStatement<E>(database: Database, val select: SelectExpression<E>) : DqlStatement<E>(database) {
    override val createEntity = select::createEntity
    override val columns = select.columns.takeIf { it.isNotEmpty() } ?: select.from?.columns
    override val usingIndex = select.columns.isNotEmpty()

    override fun SqlBuilder.doGenerateSql(params: MutableList<SqlParam<*>>) =
        appendExpression(select, params)

    override fun SqlBuilder.doGenerateCountSql(column: ColumnExpression<*, *>?, params: MutableList<SqlParam<*>>) =
        select.run { appendSqlCount(column, params) }
}

fun <E, T : QuerySourceExpression<E>> T.select(
    block: SelectExpression.Builder<E, T>.(T) -> Unit = {},
) = SelectExpression.Builder(this).apply { block(this@select) }.build()

fun <E, T : QuerySourceExpression<E>> Database.select(
    querySource: T,
    block: SelectExpression.Builder<E, T>.(T) -> Unit = {},
) = SelectStatement(this, querySource.select(block))
