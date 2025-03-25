package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.database.mapEachRow
import top.loryn.expression.*
import top.loryn.schema.Column
import top.loryn.schema.Table
import java.sql.ResultSet

data class SelectStatement(
    val database: Database,
    val columns: List<ColumnExpression<*>>,
    val from: QuerySourceExpression?,
    val where: SqlExpression<Boolean>?,
) : Statement() {
    override fun generateSql() = database.buildSql { params ->
        SelectExpression<Nothing>(columns, from, where).also { appendExpression(it, params) }
    }

    fun <R> execute(block: (ResultSet) -> R) = database.doExecute { statement ->
        statement.executeQuery().mapEachRow(block)
    }
}

@LorynDsl
class SelectBuilder<T : Table<*>>(table: T) : StatementBuilder<T, SelectStatement>(table) {
    internal val columns: MutableList<ColumnExpression<*>> = mutableListOf()
    internal var from: QuerySourceExpression? = TableExpression(table)
    internal var where: SqlExpression<Boolean>? = null

    fun <C : Any> column(column: Column<C>) {
        columns += column.also(::checkColumn)
    }

    fun columns(columns: List<Column<*>>) {
        this.columns += columns.onEach(::checkColumn)
    }

    fun columns(vararg columns: Column<*>) {
        columns(columns.toList())
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun build(database: Database) = SelectStatement(database, columns, from, where)
}

fun <T : Table<*>> Database.select(table: T, block: SelectBuilder<T>.(T) -> Unit) =
    SelectBuilder(table).apply { block(table) }.build(this)
