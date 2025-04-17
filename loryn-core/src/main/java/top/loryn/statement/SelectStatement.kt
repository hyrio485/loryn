package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.*
import top.loryn.schema.BindableQuerySource
import top.loryn.schema.QuerySource
import top.loryn.utils.SqlParamList

class SelectStatement(
    override val database: Database,
    val select: SelectExpression,
) : DqlStatement {
    override val columns = select.columns.takeIf { it.isNotEmpty() } ?: select.from?.columns
    override val usingIndex = select.columns.isNotEmpty()

    override fun doGenerateSql(builder: SqlBuilder, params: SqlParamList) {
        builder.append(select, params)
    }

    override fun doGenerateCountSql(builder: SqlBuilder, column: ColumnExpression<*>?, params: SqlParamList) {
        select.buildCountSql(builder, column, params)
    }
}

fun <T : QuerySource> Database.select(
    querySource: T,
    block: SelectExpression.Builder<T>.(T) -> Unit = {},
) = SelectStatement(this, querySource.select(block))

class BindableSelectStatement<E>(
    override val database: Database,
    val select: BindableSelectExpression<E>,
) : BindableDqlStatement<E> {
    override val createEntity = select::createEntity
    override val columns = select.columns.takeIf { it.isNotEmpty() } ?: select.from.columns
    override val usingIndex = select.columns.isNotEmpty()

    override fun doGenerateSql(builder: SqlBuilder, params: SqlParamList) {
        builder.append(select, params)
    }

    override fun doGenerateCountSql(builder: SqlBuilder, column: ColumnExpression<*>?, params: SqlParamList) {
        select.buildCountSql(builder, column, params)
    }
}

fun <E, T : BindableQuerySource<E>> Database.selectBindable(
    bindableQuerySource: T,
    block: BindableSelectExpression.Builder<E, T>.(T) -> Unit = {},
) = BindableSelectStatement(this, bindableQuerySource.selectBindable(block))
