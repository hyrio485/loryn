package top.loryn.statement

import top.loryn.database.Database
import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlExpression
import top.loryn.schema.Table
import top.loryn.support.LorynDsl
import top.loryn.utils.SqlParamList

class DeleteStatement(
    database: Database,
    val table: Table,
    val where: SqlExpression<Boolean>?,
) : DmlStatement(database) {
    override fun SqlBuilder.doGenerateSql(params: SqlParamList) = also {
        appendKeyword("DELETE").append(' ').appendKeyword("FROM").append(' ').append(table)
        where?.also { append(' ').appendKeyword("WHERE").append(' ').append(it, params) }
    }
}

@LorynDsl
class DeleteBuilder<T : Table>(table: T) : StatementBuilder<T, DeleteStatement>(table) {
    private var where: SqlExpression<Boolean>? = null

    fun where(block: (T) -> SqlExpression<Boolean>) {
        this.where = block(table)
    }

    override fun buildStatement(database: Database) = DeleteStatement(database, table, where)
}

fun <T : Table> Database.delete(table: T, block: DeleteBuilder<T>.(T) -> Unit = {}) =
    DeleteBuilder(table).apply { block(table) }.buildStatement(this).execute()

//// region 删除实体
//
//fun <T : Table> Database.delete(table: T, entity: E) =
//    delete(table) { where { it.primaryKeyFilter(entity) } }
//
//fun <T : Table> Database.delete(table: T, entities: List<E>) =
//    delete(table) { where { it.primaryKeyFilter(entities) } }
//
//// endregion
