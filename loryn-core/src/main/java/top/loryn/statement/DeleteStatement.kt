package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlExpression
import top.loryn.schema.BindableTable
import top.loryn.schema.Table
import top.loryn.support.LorynDsl
import top.loryn.utils.SqlParamList

class DeleteStatement(
    override val database: Database,
    val table: Table,
    val where: SqlExpression<Boolean>?,
) : DmlStatement {
    override fun doGenerateSql(builder: SqlBuilder, params: SqlParamList) {
        builder.appendKeywords(listOf("DELETE", "FROM"), params).append(' ').appendTable(table)
        where?.also { builder.append(' ').appendKeyword("WHERE").append(' ').append(it, params) }
    }
}

@LorynDsl
class DeleteBuilder<T : Table>(table: T) : StatementBuilder<T, DeleteStatement>(table) {
    private var where: SqlExpression<Boolean>? = null

    fun where(where: SqlExpression<Boolean>) {
        this.where = where
    }

    override fun buildStatement(database: Database) = DeleteStatement(database, table, where)
}

fun <T : Table> Database.delete(table: T, block: DeleteBuilder<T>.(T) -> Unit = {}) =
    DeleteBuilder(table).apply { block(table) }.buildStatement(this).execute()

// region 删除实体

fun <E, T : BindableTable<E>> Database.delete(table: T, entity: E) =
    delete(table) { where(it.primaryKeyFilter(entity)) }

fun <E, T : BindableTable<E>> Database.delete(table: T, entities: List<E>) =
    delete(table) { where(it.primaryKeyFilter(entities)) }

// endregion
