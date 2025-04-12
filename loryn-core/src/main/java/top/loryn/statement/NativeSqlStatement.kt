package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.BindableColumnExpression
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam
import top.loryn.utils.SqlParamList

fun Database.dml(
    sql: String,
    vararg params: SqlParam<*>,
    useGeneratedKeys: Boolean = false,
) = object : DmlStatement(this, useGeneratedKeys) {
    override fun Database.generateSql(block: SqlBuilder.(SqlParamList) -> Unit) =
        SqlAndParams(sql, params.toList())
}

fun Database.dql(
    sql: String,
    vararg params: SqlParam<*>,
    columns: List<ColumnExpression<*>> = emptyList(),
) = object : DqlStatement(this) {
    override val columns = columns.toList()

    override fun Database.generateSql(block: SqlBuilder.(SqlParamList) -> Unit) =
        SqlAndParams(sql, params.toList())
}

fun <E> Database.bindableDql(
    sql: String,
    vararg params: SqlParam<*>,
    createEntity: () -> E,
    columns: List<BindableColumnExpression<E, *>>,
) = object : BindableDqlStatement<E>(this, createEntity) {
    override val columns = columns.toList()

    override fun Database.generateSql(block: SqlBuilder.(SqlParamList) -> Unit) =
        SqlAndParams(sql, params.toList())
}
