package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
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

    fun <R> execute(block: (ResultSet) -> R): List<R> = database.doExecute { statement ->
        statement.executeQuery().use { rs ->
            mutableListOf<R>().apply {
                while (rs.next()) {
                    this += block(rs)
                }
            }
        }
    }
}

@LorynDsl
class SelectBuilder<T : Table<*>>(private val table: T) {
    internal val columns: MutableList<ColumnExpression<*>> = mutableListOf()
    internal var from: QuerySourceExpression? = TableExpression(table)
    internal var where: SqlExpression<Boolean>? = null

    fun <C : Any> addColumn(column: Column<C>) {
        columns += column
    }

    fun columns(vararg columns: Column<*>) {
        this.columns += columns
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }
}

fun <T : Table<*>> Database.select(table: T, block: SelectBuilder<T>.(T) -> Unit): SelectStatement {
    val builder = SelectBuilder(table).apply { block(table) }
    return SelectStatement(this, builder.columns, builder.from, builder.where)
}
