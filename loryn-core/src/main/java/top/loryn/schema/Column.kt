package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlParam

/**
 * 数据表的一列。
 *
 * @param T 列的数据类型。
 */
class Column<T : Any>(
    val table: Table<*>,
    val name: String,
    sqlType: SqlType<T>,
    label: String? = null,
) : ColumnExpression<T>(sqlType, label) {
    override fun SqlBuilder.generateSql(params: MutableList<SqlParam<*>>) = also {
        table.alias?.also { appendRef(it).append('.') }
        appendRef(name)
    }

    override fun toString() = "${table.alias?.let { "$it." }.orEmpty()}$name${label?.let { " AS $it" }.orEmpty()}"
}
