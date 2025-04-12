package top.loryn.schema

abstract class BindableTable<E>(
    tableName: String,
    schema: String? = null,
    category: String? = null,
    val createEntity: () -> E,
) : Table(tableName, schema, category), BindableQuerySource<E> {
    constructor(tableName: String, createEntity: () -> E) : this(tableName, null, null, createEntity)

    private fun errorColumn(name: String): Nothing = error("The $name of table $this is not specified")
    open val insertColumns: List<BindableColumn<E, *>> get() = errorColumn("insert columns")
    open val updateColumns: List<BindableColumn<E, *>> get() = errorColumn("update columns")
    open val revColumn: BindableColumn<E, Int> get() = errorColumn("revision column")
    open val deletedColumn: BindableColumn<E, Boolean> get() = errorColumn("deleted column")

    override fun createEntity() = createEntity.invoke()
}
