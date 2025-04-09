package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlExpression
import top.loryn.expression.SqlParam
import top.loryn.schema.Table
import top.loryn.support.LorynDsl

class DeleteStatement<E>(
    database: Database,
    val table: Table<E>,
    val where: SqlExpression<Boolean>?,
) : DmlStatement(database) {
    override fun SqlBuilder.doGenerateSql(params: MutableList<SqlParam<*>>) {
        appendKeyword("DELETE").append(' ').appendKeyword("FROM").append(' ').appendTable(table)
        where?.also { append(' ').appendKeyword("WHERE").append(' ').appendExpression(it, params) }
    }
}

@LorynDsl
class DeleteBuilder<E, T : Table<E>>(table: T) : StatementBuilder<T, DeleteStatement<E>>(table) {
    private var where: SqlExpression<Boolean>? = null

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun buildStatement(database: Database) = DeleteStatement(database, table, where)
}

fun <E, T : Table<E>> Database.delete(table: T, block: DeleteBuilder<E, T>.(T) -> Unit = {}) =
    DeleteBuilder(table).apply { block(table) }.buildStatement(this).execute()

// region 删除实体

fun <E, T : Table<E>> Database.delete(table: T, entity: E) =
    delete(table) { where { it.primaryKeyFilter(entity) } }

fun <E, T : Table<E>> Database.delete(table: T, entities: List<E>) =
    delete(table) { where { it.primaryKeyFilter(entities) } }

// endregion
