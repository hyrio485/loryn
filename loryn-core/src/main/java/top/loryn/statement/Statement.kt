package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlAndParams
import top.loryn.expression.SqlParam
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.support.LorynDsl
import top.loryn.support.WrappedSqlException
import top.loryn.utils.mapEachRow
import top.loryn.utils.one
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

abstract class StatementBuilder<T : Table<*>, S : Statement>(protected val table: T) {
    abstract fun buildStatement(database: Database): S
}

/**
 * 对语句的抽象。若干个表达式借助数据库及某些关键字可以组成一条语句，
 * 按返回值类型可分为查询语句（preparedStatement.executeQuery()）
 * 和更新语句（preparedStatement.executeUpdate()）。
 */
abstract class Statement(val database: Database) {
    open fun SqlBuilder.doGenerateSql(params: MutableList<SqlParam<*>>): SqlBuilder =
        throw UnsupportedOperationException("SQL generation not implemented")

    open fun Database.generateSql(
        block: SqlBuilder.(MutableList<SqlParam<*>>) -> Unit = { doGenerateSql(it) },
    ): SqlAndParams {
        val builder = dialect.newSqlBuilder(metadata.keywords, config.uppercaseKeywords).start()
        val params = mutableListOf<SqlParam<*>>()
        builder.block(params)
        return SqlAndParams(builder.build(), params)
    }

    protected inline fun <R> Database.doExecute(
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

abstract class DmlStatement(database: Database, val useGeneratedKeys: Boolean = false) : Statement(database) {
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
    open val usingIndex = true

    open fun SqlBuilder.doGenerateCountSql(
        column: ColumnExpression<*, *>?,
        params: MutableList<SqlParam<*>>,
    ): SqlBuilder = throw UnsupportedOperationException("SQL count generation not implemented")

    open fun count(
        column: ColumnExpression<*, *>? = null,
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

    fun <R> one(block: (ResultSet) -> R) = list(block).one()

    fun one() = list().one()
}

@LorynDsl
class ColumnSelectionBuilder<E>(private val table: Table<E>) {
    private val columns = mutableListOf<Column<E, *>>()

    fun addColumn(column: Column<E, *>) {
        this.columns += column.also(table::checkColumn)
    }

    fun addColumns(columns: List<Column<E, *>>) {
        this.columns += columns.onEach(table::checkColumn)
    }

    fun addColumns(vararg columns: Column<E, *>) {
        addColumns(columns.toList())
    }

    fun build(): List<Column<E, *>> {
        require(columns.isNotEmpty()) { "No columns selected" }
        return columns
    }
}

fun <E, T : Table<E>> T.selectColumns(columnsSelector: ColumnSelectionBuilder<E>.(T) -> Unit) =
    ColumnSelectionBuilder<E>(this).apply { columnsSelector(this@selectColumns) }.build()
