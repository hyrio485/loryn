package top.loryn.schema

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
    private val _columns = LinkedHashMap<String, Column<*>>()

    val columns get() = _columns.toMap()

    fun <T : Any> registerColumn(name: String, sqlType: SqlType<T>) =
        Column(this, name, sqlType).also { _columns[name] = it }

    override fun toString() = listOfNotNull(category, schema, tableName).joinToString(".")
}
