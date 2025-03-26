package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlParam
import top.loryn.expression.SqlType

/**
 * 数据表的一列。
 *
 * @param C 列的数据类型。
 */
class Column<E, C : Any>(
    val table: Table<E>,
    val name: String,
    sqlType: SqlType<C>,
    label: String? = null,
    val notNull: Boolean = false,
    setValue: ((E, C?) -> Unit)? = null,
) : ColumnExpression<E, C>(sqlType, label, setValue) {
    private fun Column<E, C>.registerColumn() = also { table.registerColumn(it) }

    fun notNull(notNull: Boolean = true) =
        Column<E, C>(table, name, sqlType, alias, notNull, setValue).registerColumn()

    fun setValue(setValue: (E, C?) -> Unit) =
        Column<E, C>(table, name, sqlType, alias, notNull, setValue).registerColumn()

    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        table.alias?.also { appendRef(it).append('.') }
        appendRef(name)
    }

    override fun toString() = "${table.alias?.let { "$it." }.orEmpty()}$name${alias?.let { "($it)" }.orEmpty()}"
}
