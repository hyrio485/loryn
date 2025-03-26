package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam
import top.loryn.schema.Table
import java.sql.PreparedStatement

abstract class StatementBuilder<T : Table<*>, S : Statement>(val table: T) {
    abstract fun build(database: Database): S
}

/**
 * 对语句的抽象。若干个表达式借助数据库及某些关键字可以组成一条语句，
 * 按返回值类型可分为查询语句（preparedStatement.executeQuery()）
 * 和更新语句（preparedStatement.executeUpdate()）。
 */
abstract class Statement {
    abstract fun generateSql(): SqlAndParams

    protected fun Database.buildSql(block: SqlBuilder.(MutableList<SqlParam<*>>) -> Unit): SqlAndParams {
        val builder = dialect.newSqlBuilder(metadata.keywords, config.uppercaseKeywords)
        val params = mutableListOf<SqlParam<*>>()
        builder.block(params)
        return SqlAndParams(builder.build(), params)
    }

    protected fun <R> Database.doExecute(useGeneratedKeys: Boolean = false, block: (PreparedStatement) -> R) =
        useConnection { conn ->
            val (sql, params) = generateSql().also(::showSql)
            val statement = if (useGeneratedKeys) {
                conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            } else {
                conn.prepareStatement(sql)
            }
            params.forEachIndexed { index, param ->
                param.setParameter(statement, index + 1)
            }
            block(statement)
        }
}
