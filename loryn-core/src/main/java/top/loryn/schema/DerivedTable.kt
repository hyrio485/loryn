package top.loryn.schema

class DerivedTable<E, T : Table<E>>(
    private val proxy: T,
    alias: String,
) : Table<E>(proxy.tableName, proxy.schema, proxy.category, alias, proxy.createEntity) {
    override val columns = proxy.columns

    override val root = proxy
}
