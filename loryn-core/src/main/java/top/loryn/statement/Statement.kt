package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.BindableColumnExpression
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.support.LorynDsl
import top.loryn.support.WrappedSqlException
import top.loryn.utils.SqlParamList
import top.loryn.utils.mapEachRow
import top.loryn.utils.one
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

abstract class StatementBuilder<T : Table, S : Statement>(protected val table: T) {
    abstract fun buildStatement(database: Database): S
}

/**
 * 对语句的抽象。若干个表达式借助数据库及某些关键字可以组成一条语句，
 * 按返回值类型可分为查询语句（preparedStatement.executeQuery()）
 * 和更新语句（preparedStatement.executeUpdate()）。
 */
interface Statement {
    val database: Database

    fun SqlBuilder.doGenerateSql(params: SqlParamList): SqlBuilder =
        throw UnsupportedOperationException("SQL generation not implemented")

    fun Database.generateSql(
        block: SqlBuilder.(SqlParamList) -> Unit = { doGenerateSql(it) },
    ): SqlAndParams {
        val builder = dialect.newSqlBuilder(metadata.keywords, config.uppercaseKeywords).start()
        val params = mutableListOf<SqlParam<*>>()
        builder.block(params)
        return SqlAndParams(builder.build(), params)
    }

    fun <R> Database.doExecute(
        useGeneratedKeys: Boolean = false,
        getSqlAndParams: () -> SqlAndParams = { database.generateSql() },
        block: (PreparedStatement) -> R,
    ) = useConnection { conn ->
        val (sql, params) = getSqlAndParams().also(::showSql)
        val statement = if (useGeneratedKeys) {
            conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
        } else {
            conn.prepareStatement(sql)
        }
        params.forEachIndexed { index, param ->
            param.setParameter(statement, index + 1)
        }
        try {
            block(statement)
        } catch (e: SQLException) {
            throw WrappedSqlException(e, sql)
        }
    }
}

interface DmlStatement : Statement {
    val useGeneratedKeys: Boolean get() = false

    fun execute(forEachGeneratedKey: (ResultSet) -> Unit = {}) =
        database.doExecute(useGeneratedKeys) { statement ->
            statement.executeUpdate().also(database::showEffects).also {
                if (useGeneratedKeys) {
                    statement.generatedKeys.mapEachRow(forEachGeneratedKey)
                }
            }
        }
}

interface DqlStatement : Statement {
    val columns: List<ColumnExpression<*>>? get() = emptyList()
    val usingIndex get() = true

    fun SqlBuilder.doGenerateCountSql(column: ColumnExpression<*>?, params: SqlParamList): SqlBuilder =
        throw UnsupportedOperationException("SQL count generation not implemented")

    fun count(
        column: ColumnExpression<*>? = null,
        getSqlAndParams: () -> SqlAndParams = {
            database.generateSql { params ->
                doGenerateCountSql(column, params)
            }
        },
    ) = database.doExecute(getSqlAndParams = getSqlAndParams) { statement ->
        statement.executeQuery().use { resultSet ->
            if (!resultSet.next()) {
                error("No result found")
            }
            resultSet.getInt(1)
        }
    }

    fun <R> list(block: (ResultSet) -> R) = database.doExecute { statement ->
        statement.executeQuery().mapEachRow(block)
    }

    fun <R> one(block: (ResultSet) -> R) = list(block).one()
}

interface BindableDqlStatement<E> : DqlStatement {
    val createEntity: () -> E
    override val columns: List<BindableColumnExpression<E, *>>? get() = emptyList()

    fun list(): List<E> {
        val columns = columns.takeUnless { it.isNullOrEmpty() }
            ?: throw UnsupportedOperationException("No columns specified")
        return list { rs ->
            createEntity().apply {
                columns.forEachIndexed { index, column ->
                    if (usingIndex) {
                        column.applyValue(this, index, rs)
                    } else {
                        column.applyValue(this, rs)
                    }
                }
            }
        }
    }

    fun one() = list().one()
}

@LorynDsl
class ColumnSelectionBuilder<C : Column<*>> {
    private val columns = mutableListOf<C>()

    fun column(column: C) {
        this.columns += column
    }

    fun columns(columns: List<C>) {
        this.columns += columns
    }

    fun columns(vararg columns: C) {
        columns(columns.toList())
    }

    fun build(): List<C> {
        require(columns.isNotEmpty()) { "No columns selected" }
        return columns
    }
}

fun <T : Table, C : Column<*>> T.selectColumns(columnsSelector: ColumnSelectionBuilder<C>.(T) -> Unit) =
    ColumnSelectionBuilder<C>().apply { columnsSelector(this@selectColumns) }.build()
