package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.expression.AssignmentExpression
import top.loryn.expression.SqlExpression
import top.loryn.expression.expr
import top.loryn.schema.Column
import top.loryn.schema.Table
import top.loryn.schema.checkTableColumn

data class UpdateStatement<E>(
    val database: Database, val table: Table<*>,
    val sets: List<AssignmentExpression<E, *>>,
    val where: SqlExpression<Boolean>?,
) : Statement() {
    init {
        require(sets.isNotEmpty()) { "At least one column must be set" }
    }

    override fun generateSql() = database.buildSql { params ->
        appendKeyword("UPDATE").append(' ').appendTable(table).append(' ').appendKeyword("SET").append(' ')
        sets.forEachIndexed { index, assignmentSqlExpression ->
            if (index > 0) append(", ")
            appendExpression(assignmentSqlExpression, params)
        }
        where?.also {
            append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params)
        }
    }

    fun execute() = database.doExecute { statement ->
        statement.executeUpdate().also(database::showEffects)
    }
}

@LorynDsl
class UpdateBuilder<E, T : Table<E>>(table: T) : StatementBuilder<T, UpdateStatement<E>>(table) {
    internal val sets = mutableListOf<AssignmentExpression<E, *>>()
    internal var where: SqlExpression<Boolean>? = null

    fun <C : Any> set(column: Column<E, C>, value: C?) {
        checkTableColumn(table, column)
        sets += AssignmentExpression(column, column.expr(value))
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun build(database: Database) = UpdateStatement(database, table, sets, where)
}

fun <E, T : Table<E>> Database.update(
    table: T,
    block: UpdateBuilder<E, T>.(T) -> Unit,
) = UpdateBuilder(table).apply { block(table) }.build(this)
