package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam
import java.sql.PreparedStatement

abstract class Statement {
    abstract fun generateSql(): SqlAndParams

    protected fun Database.buildSql(block: SqlBuilder.(MutableList<SqlParam<*>>) -> Unit): SqlAndParams {
        val builder = dialect.newSqlBuilder(metadata.keywords, config.uppercaseKeywords)
        val params = mutableListOf<SqlParam<*>>()
        builder.block(params)
        return SqlAndParams(builder.build(), params)
    }

    protected fun <R> Database.doExecute(block: (PreparedStatement) -> R) = useConnection { conn ->
        val (sql, params) = generateSql().also(::showSql)
        val statement = conn.prepareStatement(sql)
        params.forEachIndexed { index, param ->
            param.setParameter(statement, index + 1)
        }
        block(statement)
    }
}
