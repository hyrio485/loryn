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
) : QuerySourceExpression<E>() {
    private val columnsMapMutable = LinkedHashMap<String, Column<E, *>>()

    override val columns = columnsMapMutable.values.toList()

    fun <C : Any> registerColumn(column: Column<E, C>) {
        columnsMapMutable[column.name] = column
    }

    fun <C : Any> registerColumn(name: String, sqlType: SqlType<C>) =
        Column<E, C>(this, name, sqlType).also(this::registerColumn)

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendTable(this@Table)
        alias?.also { append(' ').appendRef(it) }
    }

    override fun toString() = listOfNotNull(category, schema, tableName).joinToString(".")
}
