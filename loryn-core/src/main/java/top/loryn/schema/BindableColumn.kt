package top.loryn.schema

import top.loryn.expression.BindableColumnExpression
import top.loryn.support.SqlType
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

class BindableColumn<E, T>(
    table: BindableTable<E>,
    name: String,
    sqlType: SqlType<T>,
    override val getter: (E.() -> T?)?,
    override val setter: (E.(T?) -> Unit)?,
    primaryKey: Boolean = false,
    notNull: Boolean = false,
) : Column<T>(table, name, sqlType, primaryKey, notNull), BindableColumnExpression<E, T> {
    constructor(
        table: BindableTable<E>,
        name: String,
        sqlType: SqlType<T>,
        property: KMutableProperty1<E, T?>,
        primaryKey: Boolean = false,
        notNull: Boolean = false,
    ) : this(table, name, sqlType, { property.get(this) }, { property.set(this, it) }, primaryKey, notNull)

    constructor(
        table: BindableTable<E>,
        name: String,
        sqlType: SqlType<T>,
        property: KProperty1<E, T?>,
        primaryKey: Boolean = false,
        notNull: Boolean = false,
    ) : this(table, name, sqlType, { property.get(this) }, null, primaryKey, notNull)
}
