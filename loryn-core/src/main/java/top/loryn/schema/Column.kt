package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlParam
import top.loryn.expression.SqlType

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
    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        table.alias?.also { appendRef(it).append('.') }
        appendRef(name)
    }

    override fun toString() = "${table.alias?.let { "$it." }.orEmpty()}$name${label?.let { "($it)" }.orEmpty()}"
}
