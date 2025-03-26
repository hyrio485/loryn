package top.loryn.statement

import top.loryn.database.Database
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam

fun Database.dml(
    sql: String,
    vararg params: SqlParam<*>,
    useGeneratedKeys: Boolean = false,
) = object : DmlStatement(this, useGeneratedKeys) {
    override fun generateSql() = SqlAndParams(sql, params.toList())
}

fun <E> Database.dql(
    sql: String,
    vararg params: SqlParam<*>,
    createEntity: (() -> E)? = null,
    columns: List<ColumnExpression<E, *>>,
) = object : DqlStatement<E>(this) {
    override val createEntity = createEntity
    override val columns = columns.toList()

    override fun generateSql() = SqlAndParams(sql, params.toList())
}
