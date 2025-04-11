package top.loryn.schema

abstract class BindableTable<E>(
    tableName: String,
    schema: String? = null,
    category: String? = null,
    val createEntity: () -> E,
) : Table(tableName, schema, category), BindableQuerySource<E> {
    constructor(tableName: String, createEntity: () -> E) : this(tableName, null, null, createEntity)

//    open val insertColumns: List<Column<E, *>> get() = errorColumn("insert columns")
//    open val updateColumns: List<Column<E, *>> get() = errorColumn("update columns")
//    open val revColumn: Column<E, Int> get() = errorColumn("revision column")
//    open val deletedColumn: Column<E, Boolean> get() = errorColumn("deleted column")

    override fun createEntity() = createEntity.invoke()
}
