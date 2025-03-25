package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.expression.AssignmentExpression
import top.loryn.expression.SqlExpression
import top.loryn.expression.expr
import top.loryn.schema.Column
import top.loryn.schema.Table

data class UpdateStatement(
    val database: Database, val table: Table<*>,
    val sets: List<AssignmentExpression<*>>,
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
class UpdateBuilder<T : Table<*>>(table: T) : StatementBuilder<T, UpdateStatement>(table) {
    internal val sets = mutableListOf<AssignmentExpression<*>>()
    internal var where: SqlExpression<Boolean>? = null

    fun <V : Any> set(column: Column<V>, value: V?) {
        checkColumn(column)
        sets += AssignmentExpression(column, column.expr(value))
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun build(database: Database) = UpdateStatement(database, table, sets, where)
}

fun <T : Table<*>> Database.update(table: T, block: UpdateBuilder<T>.(T) -> Unit) =
    UpdateBuilder(table).apply { block(table) }.build(this)
