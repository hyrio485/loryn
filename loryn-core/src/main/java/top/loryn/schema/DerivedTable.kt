package top.loryn.schema

class DerivedTable<E, T : Table<E>>(
    private val proxy: T,
    alias: String,
) : Table<E>(proxy.tableName, proxy.schema, proxy.category, alias, proxy.createEntity) {
    operator fun <C : Any> invoke(block: T.() -> Column<E, C>) = DerivedColumn(proxy.block())

    override val columns = proxy.columns

    override val root = proxy
}
