package top.loryn.schema

import top.loryn.expression.SqlType
import java.util.*

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
) {
    private val columnsMutable = LinkedHashMap<String, Column<*>>()

    val columns: Map<String, Column<*>> = Collections.unmodifiableMap(columnsMutable)

    fun <T : Any> registerColumn(name: String, sqlType: SqlType<T>) =
        Column(this, name, sqlType).also { columnsMutable[name] = it }

    override fun toString() = listOfNotNull(category, schema, tableName).joinToString(".")
}
