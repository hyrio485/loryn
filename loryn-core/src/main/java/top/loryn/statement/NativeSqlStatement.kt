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
    params: List<SqlParam<*>>,
    useGeneratedKeys: Boolean = false,
) = object : DmlStatement {
    override val database = this@dml

    override val useGeneratedKeys = useGeneratedKeys

    override fun doGenerateSql(builder: SqlBuilder, params: SqlParamList) {
        throw UnsupportedOperationException("Not needed")
    }

    override fun Database.generateSql(block: (SqlBuilder, SqlParamList) -> Unit) =
        SqlAndParams(sql, params)
}

fun Database.dml(
    sql: String,
    vararg params: SqlParam<*>,
    useGeneratedKeys: Boolean = false,
) = dml(sql, params.toList(), useGeneratedKeys)

fun Database.dql(
    sql: String,
    params: List<SqlParam<*>>,
    columns: List<ColumnExpression<*>> = emptyList(),
) = object : DqlStatement {
    override val database = this@dql
    override val columns = columns

    override fun doGenerateCountSql(builder: SqlBuilder, column: ColumnExpression<*>?, params: SqlParamList) =
        throw UnsupportedOperationException("Query using native SQL does not support count")

    override fun doGenerateSql(builder: SqlBuilder, params: SqlParamList) =
        throw UnsupportedOperationException("Not needed")

    override fun Database.generateSql(block: SqlBuilder.(SqlParamList) -> Unit) =
        SqlAndParams(sql, params)
}

fun Database.dql(
    sql: String,
    vararg params: SqlParam<*>,
    columns: List<ColumnExpression<*>> = emptyList(),
) = dql(sql, params.toList(), columns)

fun <E> Database.dqlBindable(
    sql: String,
    params: List<SqlParam<*>>,
    createEntity: () -> E,
    columns: List<BindableColumnExpression<E, *>>,
) = object : BindableDqlStatement<E> {
    override val database = this@dqlBindable
    override val createEntity = createEntity
    override val columns = columns

    override fun doGenerateCountSql(builder: SqlBuilder, column: ColumnExpression<*>?, params: SqlParamList) =
        throw UnsupportedOperationException("Query using native SQL does not support count")

    override fun doGenerateSql(builder: SqlBuilder, params: SqlParamList) =
        throw UnsupportedOperationException("Not needed")

    override fun Database.generateSql(block: SqlBuilder.(SqlParamList) -> Unit) =
        SqlAndParams(sql, params)
}

fun <E> Database.dqlBindable(
    sql: String,
    vararg params: SqlParam<*>,
    createEntity: () -> E,
    columns: List<BindableColumnExpression<E, *>>,
) = dqlBindable(sql, params.toList(), createEntity, columns)
