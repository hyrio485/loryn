package top.loryn.schema

class DerivedColumn<E, C : Any>(
    proxy: Column<E, C>,
    table: Table<E>? = proxy.table,
    alias: String? = proxy.alias,
    primaryKey: Boolean = proxy.primaryKey,
    notNull: Boolean = proxy.notNull,
    setter: (E.(C?) -> Unit)? = proxy.setter,
    getter: (E.() -> C?)? = proxy.getter,
) : Column<E, C>(proxy.name, proxy.sqlType, table, alias, primaryKey, notNull, setter, getter) {
    override val root = proxy
}
