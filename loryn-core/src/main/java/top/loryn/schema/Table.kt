package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.QuerySourceExpression
import top.loryn.expression.SqlParam
import top.loryn.expression.SqlType

/**
 * 数据表。
 *
 * @param E 表绑定的实体类类型。
 */
abstract class Table<E>(
    val tableName: String,
    val schema: String? = null,
    val category: String? = null,
    val alias: String? = null,
    val newEntity: (() -> E)? = null,
) : QuerySourceExpression() {
    private val columnsMapMutable = LinkedHashMap<String, Column<*>>()

    override val columns = columnsMapMutable.values.toList()

    fun <T : Any> registerColumn(column: Column<T>) =
        column.also { columnsMapMutable[column.name] = it }

    fun <T : Any> registerColumn(name: String, sqlType: SqlType<T>) =
        registerColumn(Column(this, name, sqlType))

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendTable(this@Table)
        alias?.also { append(' ').appendRef(it) }
    }

    override fun toString() = listOfNotNull(category, schema, tableName).joinToString(".")
}
