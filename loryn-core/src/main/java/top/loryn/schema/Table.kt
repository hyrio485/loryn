package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.support.WithAlias
import top.loryn.support.WithAlias.Companion.getAliasOrNull
import top.loryn.utils.SqlParamList

abstract class Table(
    val tableName: String,
    val schema: String? = null,
    val category: String? = null,
) : QuerySource {
    // region columns

    private val columnsMapMutable = mutableListOf<Column<*>>()

    override val columns get() = columnsMapMutable.toList()

    /** 过滤出所有主键列（可能为空） */
    open val primaryKeysMayEmpty get() = columnsMapMutable.filter { it.primaryKey }

    /** 所有主键列（包含一个或多个，没有定义主键时抛出异常） */
    open val primaryKeys
        get() = primaryKeysMayEmpty.takeIf { it.isNotEmpty() }
            ?: error("Table $this does not have a primary key")

    /** 主键列（只有一个时才不为空） */
    open val primaryKeySingleOrNull get() = primaryKeys.singleOrNull()

    /** 主键列（非只有一个时抛出异常） */
    open val primaryKey
        get() = primaryKeys.singleOrNull()
            ?: error("Table $this has more than one primary keys, use primaryKeys instead")

    open fun <T> column(name: String, sqlType: SqlType<T>, primaryKey: Boolean = false, notNull: Boolean = false) =
        Column(this, name, sqlType, primaryKey, notNull).also { columnsMapMutable += it }

    // endregion

    override fun SqlBuilder.appendSql(params: SqlParamList) =
        append(this@Table).appendAliasUsingAs(this@Table)

    override fun toString() = listOfNotNull(category, schema, tableName).joinToString(".")

    override fun <E> bind(createEntity: () -> E): BindableTable<E> =
        object : BindableTable<E>(tableName, schema, category, createEntity), WithAlias {
            private val this0 = this@Table

            override val alias = this0.getAliasOrNull()

            override fun SqlBuilder.appendSql(params: SqlParamList) = with(this0) { appendSql(params) }
        }

    override fun aliased(alias: String): Table =
        object : Table(tableName, schema, category), WithAlias {
            private val this0 = this@Table

            override val alias = alias

            override fun SqlBuilder.appendSql(params: SqlParamList) = with(this0) { appendSql(params) }
        }

    operator fun <T> get(column: Column<T>) =
        Column(this, column.name, column.sqlType, column.primaryKey, column.notNull)

    operator fun <T> get(columns: List<Column<T>>) =
        columns.map { this[it] }
}
