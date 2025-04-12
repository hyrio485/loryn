package top.loryn.schema

import top.loryn.expression.BindableColumnExpression
import top.loryn.support.SqlType
import kotlin.reflect.KMutableProperty1

class BindableColumn<E, T>(
    table: BindableTable<E>,
    name: String,
    sqlType: SqlType<T>,
    primaryKey: Boolean = false,
    notNull: Boolean = false,
    override val getter: E.() -> T?,
    override val setter: E.(T?) -> Unit,
) : Column<T>(table, name, sqlType, primaryKey, notNull), BindableColumnExpression<E, T> {
    constructor(
        table: BindableTable<E>,
        name: String,
        sqlType: SqlType<T>,
        primaryKey: Boolean = false,
        notNull: Boolean = false,
        property: KMutableProperty1<E, T?>,
    ) : this(
        table, name, sqlType, primaryKey, notNull,
        { property.get(this) }, { property.set(this, it) }
    )
}
