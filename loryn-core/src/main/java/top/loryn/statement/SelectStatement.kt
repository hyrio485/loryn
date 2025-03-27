package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.expression.ColumnExpression
import top.loryn.expression.QuerySourceExpression
import top.loryn.expression.SelectExpression
import top.loryn.expression.SqlExpression
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.utils.checkTableColumn

class SelectStatement<E>(
    database: Database,
    val select: SelectExpression<E>,
) : DqlStatement<E>(database) {
    override val createEntity = select::createEntity
    override val columns = select.columns.takeIf { it.isNotEmpty() } ?: select.from?.columns

    override fun generateSql() = database.buildSql { params ->
        appendExpression(select, params)
    }
}

@LorynDsl
class SelectBuilder<E, T : Table<E>>(table: T) : StatementBuilder<T, SelectStatement<E>>(table) {
    internal val columns: MutableList<ColumnExpression<E, *>> = mutableListOf()
    internal var from: QuerySourceExpression<E>? = table
    internal var where: SqlExpression<Boolean>? = null

    fun <C : Any> column(column: Column<E, C>) {
        columns += column.also { checkTableColumn(table, it) }
    }

    fun columns(columns: List<Column<E, *>>) {
        this.columns += columns.onEach { checkTableColumn(table, it) }
    }

    fun columns(vararg columns: Column<E, *>) {
        columns(columns.toList())
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun buildStatement(database: Database) =
        SelectStatement(database, SelectExpression(columns, from, where))
}

fun <E, T : Table<E>> Database.select(table: T, block: SelectBuilder<E, T>.(T) -> Unit = {}) =
    SelectBuilder(table).apply { block(table) }.buildStatement(this)
