package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.SqlParam
import top.loryn.support.SqlType

class DerivedColumn<E, C : Any>(
    val proxy: ColumnExpression<E, C>,
    alias: String? = proxy.alias,
    label: String? = proxy.label,
    sqlTypeNullable: SqlType<C>? = proxy.sqlTypeNullable,
    setter: (E.(C?) -> Unit)? = proxy.setter,
) : ColumnExpression<E, C>(alias, label, sqlTypeNullable, setter) {
    override val tableColumn = proxy as? Column<E, C>

    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) =
        with(proxy) { appendSqlOriginal(params) }
}
