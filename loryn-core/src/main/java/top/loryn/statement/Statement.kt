package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.database.mapEachRow
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam
import top.loryn.schema.Table
import top.loryn.utils.one
import java.sql.PreparedStatement
import java.sql.ResultSet

abstract class StatementBuilder<T : Table<*>, S : Statement>(val table: T) {
    abstract fun buildStatement(database: Database): S
}

/**
 * 对语句的抽象。若干个表达式借助数据库及某些关键字可以组成一条语句，
 * 按返回值类型可分为查询语句（preparedStatement.executeQuery()）
 * 和更新语句（preparedStatement.executeUpdate()）。
 */
abstract class Statement(val database: Database) {
    abstract fun generateSql(): SqlAndParams

    protected fun Database.buildSql(block: SqlBuilder.(MutableList<SqlParam<*>>) -> Unit): SqlAndParams {
        val builder = dialect.newSqlBuilder(metadata.keywords, config.uppercaseKeywords).start()
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

abstract class DmlStatement(database: Database, val useGeneratedKeys: Boolean = false) : Statement(database) {
    @JvmOverloads
    open fun execute(forEachGeneratedKey: (ResultSet) -> Unit = {}) =
        database.doExecute(useGeneratedKeys) { statement ->
            statement.executeUpdate().also(database::showEffects).also {
                if (useGeneratedKeys) {
                    statement.generatedKeys.mapEachRow(forEachGeneratedKey)
                }
            }
        }
}

abstract class DqlStatement<E>(database: Database) : Statement(database) {
    open val createEntity: (() -> E)? = null
    open val columns: List<ColumnExpression<E, *>>? = emptyList()

    open fun <R> list(block: (ResultSet) -> R) = database.doExecute { statement ->
        statement.executeQuery().mapEachRow(block)
    }

    open fun list(): List<E> {
        val createEntity = createEntity ?: throw UnsupportedOperationException(
            "Entity creation method is not specified"
        )
        val columns = columns.takeUnless { it.isNullOrEmpty() }
            ?: throw UnsupportedOperationException("No columns specified")
        return list { rs ->
            createEntity().apply {
                columns.forEachIndexed { index, column -> column.applyValue(this, index, rs) }
            }
        }
    }

    fun <R> one(block: (ResultSet) -> R) = list(block).one()

    fun one() = list().one()
}
