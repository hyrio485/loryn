package top.loryn.schema

import top.loryn.support.SqlType

class BindableColumn<E, T>(
    table: BindableTable<E>,
    name: String,
    sqlType: SqlType<T>,
    primaryKey: Boolean = false,
    notNull: Boolean = false,
    setter: (E.(T?) -> Unit)? = null,
    getter: (E.() -> T?)? = null,
) : Column<T>(table, name, sqlType, primaryKey, notNull) {
}
