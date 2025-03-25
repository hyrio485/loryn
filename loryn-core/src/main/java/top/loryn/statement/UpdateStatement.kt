package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.expression.AssignmentExpression
import top.loryn.expression.ParameterExpression
import top.loryn.expression.SqlExpression
import top.loryn.schema.Column
import top.loryn.schema.Table

data class UpdateStatement(
    val database: Database, val table: Table<*>,
    val sets: List<AssignmentExpression<*>>,
    val where: SqlExpression<Boolean>?,
) : Statement() {
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
class UpdateBuilder<T : Table<*>>(private val table: T) {
    internal val sets = mutableListOf<AssignmentExpression<*>>()
    internal var where: SqlExpression<Boolean>? = null

    fun <V : Any> set(column: Column<V>, value: V?) {
        sets += AssignmentExpression(column, ParameterExpression(value, column.sqlType))
    }

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }
}

fun <T : Table<*>> Database.update(table: T, block: UpdateBuilder<T>.(T) -> Unit): UpdateStatement {
    val builder = UpdateBuilder(table).apply { block(table) }
    return UpdateStatement(this, table, builder.sets, builder.where)
}
