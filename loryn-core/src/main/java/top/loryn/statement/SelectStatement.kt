package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.BindableSelectExpression
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SelectExpression
import top.loryn.schema.BindableQuerySource
import top.loryn.schema.QuerySource
import top.loryn.utils.SqlParamList

class SelectStatement(database: Database, val select: SelectExpression) : DqlStatement(database) {
    override val columns = select.columns.takeIf { it.isNotEmpty() } ?: select.from?.columns
    override val usingIndex = select.columns.isNotEmpty()

    override fun SqlBuilder.doGenerateSql(params: SqlParamList) =
        append(select, params)

    override fun SqlBuilder.doGenerateCountSql(column: ColumnExpression<*>?, params: SqlParamList) =
        select.run { appendSqlCount(column, params) }
}

fun <T : QuerySource> T.select(
    block: SelectExpression.Builder<T>.(T) -> Unit = {},
) = SelectExpression.Builder(this).apply { block(this@select) }.build()

fun <T : QuerySource> Database.select(
    querySource: T,
    block: SelectExpression.Builder<T>.(T) -> Unit = {},
) = SelectStatement(this, querySource.select(block))

class BindableSelectStatement<E>(
    database: Database, val select: BindableSelectExpression<E>,
) : BindableDqlStatement<E>(database, select::createEntity) {
    override val columns = select.columns.takeIf { it.isNotEmpty() } ?: select.from.columns
    override val usingIndex = select.columns.isNotEmpty()

    override fun SqlBuilder.doGenerateSql(params: SqlParamList) =
        append(select, params)

    override fun SqlBuilder.doGenerateCountSql(column: ColumnExpression<*>?, params: SqlParamList) =
        select.run { appendSqlCount(column, params) }
}

fun <E, T : BindableQuerySource<E>> T.selectBindable(
    block: BindableSelectExpression.Builder<E, T>.(T) -> Unit = {},
) = BindableSelectExpression.Builder(this).apply { block(this@selectBindable) }.build()

fun <E, T : BindableQuerySource<E>> Database.selectBindable(
    bindableQuerySource: T,
    block: BindableSelectExpression.Builder<E, T>.(T) -> Unit = {},
) = BindableSelectStatement(this, bindableQuerySource.selectBindable(block))
