package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam
import top.loryn.schema.Column
import top.loryn.schema.Table
import java.sql.PreparedStatement

abstract class StatementBuilder<T : Table<*>, S : Statement>(val table: T) {
    fun checkColumn(column: Column<*>) {
        require(column.table === table) { "Column $column does not belong to table $table" }
    }

    abstract fun build(database: Database): S
}

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
