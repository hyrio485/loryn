package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.LorynDsl
import top.loryn.expression.SqlExpression
import top.loryn.expression.and
import top.loryn.expression.eq
import top.loryn.schema.Table

class DeleteStatement<E>(
    database: Database,
    val table: Table<E>,
    val where: SqlExpression<Boolean>?,
) : DmlStatement(database) {
    override fun generateSql() = database.buildSql { params ->
        appendKeyword("DELETE").append(' ').appendKeyword("FROM").append(' ').appendTable(table)
        where?.also { append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params) }
    }
}

@LorynDsl
class DeleteBuilder<E, T : Table<E>>(table: T) : StatementBuilder<T, DeleteStatement<E>>(table) {
    var where: SqlExpression<Boolean>? = null

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun buildStatement(database: Database) = DeleteStatement(database, table, where)
}

fun <E, T : Table<E>> Database.delete(table: T, block: DeleteBuilder<E, T>.(T) -> Unit = {}) =
    DeleteBuilder(table).apply { block(table) }.buildStatement(this)

fun <E, T : Table<E>> Database.delete(table: T, entity: E) = delete(table) {
    where {
        it.primaryKeys.map { primaryKey ->
            primaryKey.getValueAndTransform(entity) { column, expr -> column eq expr }
        }.reduce { acc, expr -> acc.and<E>(expr) }
    }
}
