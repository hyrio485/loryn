package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.expression.AssignmentExpression
import top.loryn.expression.SqlExpression
import top.loryn.expression.and
import top.loryn.expression.eq
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.utils.checkTableColumn

class UpdateStatement<E>(
    database: Database,
    val table: Table<E>,
    val sets: List<AssignmentExpression<E, *>>,
    val where: SqlExpression<Boolean>?,
) : DmlStatement(database) {
    init {
        require(sets.isNotEmpty()) { "At least one column must be set" }
    }

    override fun generateSql() = database.buildSql { params ->
        appendKeyword("UPDATE").append(' ').appendTable(table).append(' ')
        appendKeyword("SET").append(' ').appendExpressions(sets, params)
        where?.also { append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params) }
    }
}

@LorynDsl
class UpdateBuilder<E, T : Table<E>>(table: T) : StatementBuilder<T, UpdateStatement<E>>(table) {
    internal val sets = mutableListOf<AssignmentExpression<E, *>>()
    internal var where: SqlExpression<Boolean>? = null

    fun <C : Any> set(column: Column<E, C>, value: SqlExpression<C>) {
        checkTableColumn(table, column)
        sets += AssignmentExpression(column, value)
    }

    fun <C : Any> set(column: Column<E, C>, block: (Column<E, C>) -> SqlExpression<C>) {
        set(column, block(column))
    }

    fun <C : Any> set(column: Column<E, C>, value: C?) {
        set(column, column.expr(value))
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun buildStatement(database: Database) = UpdateStatement(database, table, sets, where)
}

fun <E, T : Table<E>> Database.update(
    table: T,
    block: UpdateBuilder<E, T>.(T) -> Unit = {},
) = UpdateBuilder(table).apply { block(table) }.buildStatement(this).execute()

fun <E, T : Table<E>> Database.update(table: T, entity: E, columns: List<Column<E, *>> = table.updateColumns) =
    update(table) {
        columns.forEach { column ->
            set(column) { it.getValueExpr(entity) }
            where {
                it.primaryKeys.map { primaryKey ->
                    primaryKey.getValueAndTransform(entity) { column, expr -> column eq expr }
                }.reduce { acc, expr -> acc.and<E>(expr) }
            }
        }
    }

fun <E, T : Table<E>> Database.update(table: T, entity: E, vararg columns: Column<E, *>) =
    update(table, entity, columns.toList())
