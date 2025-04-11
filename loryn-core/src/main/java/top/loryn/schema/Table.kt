package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.SqlParam
import top.loryn.support.SqlType

abstract class Table(
    val tableName: String,
    val schema: String? = null,
    val category: String? = null,
) : QuerySource {
    private val columnsMapMutable = LinkedHashMap<String, Column<*>>()

    override val columns get() = columnsMapMutable.values.toList()

    fun <C> registerColumn(column: Column<C>) {
        columnsMapMutable[column.name] = column
    }

    fun <C> registerColumn(name: String, sqlType: SqlType<C>) =
        Column<C>(this, name, sqlType).also(this::registerColumn)

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) =
        append(this@Table).appendAlias(this@Table) { appendKeyword("AS").append(' ').appendRef(it) }
}
