package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.and
import top.loryn.expression.eq
import top.loryn.expression.`in`
import top.loryn.support.SqlType
import top.loryn.support.Tuple
import top.loryn.support.WithAlias
import top.loryn.utils.SqlParamList
import kotlin.reflect.KMutableProperty1

abstract class BindableTable<E>(
    tableName: String,
    schema: String? = null,
    category: String? = null,
    val createEntity: () -> E,
) : Table(tableName, schema, category), BindableQuerySource<E> {
    constructor(tableName: String, createEntity: () -> E) : this(tableName, null, null, createEntity)

    // region columns

    private val columnsMapMutable = mutableListOf<BindableColumn<E, *>>()

    override val columns get() = columnsMapMutable.toList()

    /** 过滤出所有主键列（可能为空） */
    override val primaryKeysMayEmpty get() = columnsMapMutable.filter { it.primaryKey }

    /** 所有主键列（包含一个或多个，没有定义主键时抛出异常） */
    override val primaryKeys
        get() = primaryKeysMayEmpty.takeIf { it.isNotEmpty() }
            ?: error("Table $this does not have a primary key")

    /** 主键列（只有一个时才不为空） */
    override val primaryKeySingleOrNull get() = primaryKeys.singleOrNull()

    /** 主键列（非只有一个时抛出异常） */
    override val primaryKey
        get() = primaryKeys.singleOrNull()
            ?: error("Table $this has more than one primary keys, use primaryKeys instead")

    fun primaryKeyFilter(entity: E) = primaryKeys.map { primaryKey ->
        primaryKey.getValueAndTransform(entity) { column, expr -> column eq expr }
    }.reduce { acc, expr -> acc and expr }

    fun primaryKeyFilter(entities: List<E>) = primaryKeys.let { primaryKeys ->
        Tuple(primaryKeys) `in` entities.map { entity ->
            Tuple(primaryKeys.map { it.getValueExpr(entity) })
        }
    }

    override fun <T> column(name: String, sqlType: SqlType<T>, primaryKey: Boolean, notNull: Boolean) =
        throw UnsupportedOperationException("For BindableTable, please add a BindableColumn instead of a normal column.")

    fun <T> column(
        name: String,
        sqlType: SqlType<T>,
        getter: E.() -> T?,
        setter: E.(T?) -> Unit,
        primaryKey: Boolean = false,
        notNull: Boolean = false,
    ) = BindableColumn(this, name, sqlType, primaryKey, notNull, getter, setter).also { columnsMapMutable += it }

    fun <T> column(
        name: String,
        sqlType: SqlType<T>,
        property: KMutableProperty1<E, T?>,
        primaryKey: Boolean = false,
        notNull: Boolean = false,
    ) = BindableColumn(this, name, sqlType, primaryKey, notNull, property).also { columnsMapMutable += it }

    // endregion

    private fun errorColumn(name: String): Nothing = error("The $name of table $this is not specified")

    open val insertColumns: List<BindableColumn<E, *>> get() = errorColumn("insert columns")
    open val updateColumns: List<BindableColumn<E, *>> get() = errorColumn("update columns")
    open val revColumn: BindableColumn<E, Int> get() = errorColumn("revision column")
    open val deletedColumn: BindableColumn<E, Boolean> get() = errorColumn("deleted column")

    override fun createEntity() = createEntity.invoke()

    override fun aliased(alias: String): BindableTable<E> =
        object : BindableTable<E>(tableName, schema, category, createEntity), WithAlias {
            private val this0 = this@BindableTable
            override val alias = alias

            override fun SqlBuilder.appendSql(params: SqlParamList) = with(this0) { appendSql(params) }
        }

    operator fun <T> get(column: BindableColumn<E, T>) = BindableColumn(
        this, column.name, column.sqlType, column.primaryKey, column.notNull, column.getter, column.setter
    )
}
