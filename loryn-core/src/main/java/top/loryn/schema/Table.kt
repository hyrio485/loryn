package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList

abstract class Table(
    val tableName: String,
    val schema: String? = null,
    val category: String? = null,
) : QuerySource {
    private val columnsMapMutable = LinkedHashMap<String, Column<*>>()

    override val columns get() = columnsMapMutable.values.toList()

    /** 过滤出所有主键列（可能为空） */
    val primaryKeysMayEmpty get() = columns.filter { it.primaryKey }

    /** 所有主键列（包含一个或多个，没有定义主键时抛出异常） */
    val primaryKeys
        get() = primaryKeysMayEmpty.takeIf { it.isNotEmpty() }
            ?: error("Table $this does not have a primary key")

    /** 主键列（只有一个时才不为空） */
    val primaryKeySingleOrNull get() = primaryKeys.singleOrNull()

    /** 主键列（非只有一个时抛出异常） */
    val primaryKey
        get() = primaryKeys.singleOrNull()
            ?: error("Table $this has more than one primary keys, use primaryKeys instead")

    fun <T> registerColumn(column: Column<T>) {
        columnsMapMutable[column.name] = column
    }

    fun <T> registerColumn(name: String, sqlType: SqlType<T>) =
        Column<T>(this, name, sqlType).also(this::registerColumn)

    override fun SqlBuilder.appendSql(params: SqlParamList) =
        append(this@Table).appendAlias(this@Table) { appendKeyword("AS").append(' ').appendRef(it) }

    override fun toString() = listOfNotNull(category, schema, tableName).joinToString(".")
}
