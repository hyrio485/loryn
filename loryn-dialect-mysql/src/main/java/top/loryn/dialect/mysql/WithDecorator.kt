package top.loryn.dialect.mysql

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SelectExpression
import top.loryn.schema.Table
import top.loryn.statement.BindableDqlStatement
import top.loryn.statement.DmlStatement
import top.loryn.statement.DqlStatement
import top.loryn.utils.SqlParamList

private fun SqlBuilder.appendWithClause(
    selects: List<Pair<Table, SelectExpression>>,
    params: SqlParamList,
) = also {
    appendKeyword("WITH").append(' ').appendList(selects, params) { (table, select), params ->
        appendRef(table.tableName).append(' ').appendKeyword("AS").append(' ')
        append('(').append(select, params).append(')')
    }.append(' ')
}

class WithDmlDecorator(
    val selects: List<Pair<Table, SelectExpression>>,
    val statement: DmlStatement,
) : DmlStatement {
    constructor(
        vararg selects: Pair<Table, SelectExpression>,
        statement: DmlStatement,
    ) : this(selects.toList(), statement)

    override val database = statement.database

    override fun doGenerateSql(builder: SqlBuilder, params: SqlParamList) {
        builder.appendWithClause(selects, params)
        statement.doGenerateSql(builder, params)
    }
}

abstract class BaseWithDqlDecorator(
    override val database: Database,
) : DqlStatement {
    abstract val selects: List<Pair<Table, SelectExpression>>
    abstract val statement: DqlStatement

    override fun doGenerateSql(builder: SqlBuilder, params: SqlParamList) {
        builder.appendWithClause(selects, params)
        statement.doGenerateSql(builder, params)
    }

    override fun doGenerateCountSql(
        builder: SqlBuilder,
        column: ColumnExpression<*>?, params: SqlParamList,
    ) {
        builder.appendWithClause(selects, params)
        statement.doGenerateCountSql(builder, column, params)
    }
}

class WithDqlDecorator(
    override val selects: List<Pair<Table, SelectExpression>>,
    override val statement: DqlStatement,
) : BaseWithDqlDecorator(statement.database), DqlStatement {
    override val columns = statement.columns
    override val usingIndex = statement.usingIndex

    constructor(
        vararg selects: Pair<Table, SelectExpression>,
        statement: DqlStatement,
    ) : this(selects.toList(), statement)
}

class WithBindableDqlDecorator<E>(
    override val selects: List<Pair<Table, SelectExpression>>,
    override val statement: BindableDqlStatement<E>,
) : BaseWithDqlDecorator(statement.database), BindableDqlStatement<E> {
    override val createEntity = statement.createEntity
    override val columns = statement.columns
    override val usingIndex = statement.usingIndex

    constructor(
        vararg selects: Pair<Table, SelectExpression>,
        statement: BindableDqlStatement<E>,
    ) : this(selects.toList(), statement)
}
