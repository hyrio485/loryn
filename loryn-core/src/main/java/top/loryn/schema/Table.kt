package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.QuerySourceExpression
import top.loryn.expression.SqlParam
import top.loryn.expression.SqlType

/**
 * 数据表。
 *
 * @param E 表绑定的实体类类型。
 */
abstract class Table<E>(
    val tableName: String,
    val schema: String? = null,
    val category: String? = null,
    val alias: String? = null,
    val createEntity: (() -> E)? = null,
) : QuerySourceExpression<E>() {
    private val columnsMapMutable = LinkedHashMap<String, Column<E, *>>()

    override val columns get() = columnsMapMutable.values.toList()

    /** 过滤出所有主键列（可能为空） */
    val primaryKeysMayEmpty get() = columns.filter { it.primaryKey }

    /** 所有主键列（包含一个或多个，没有定义主键时抛出异常） */
    val primaryKeys
        get() = primaryKeysMayEmpty.takeIf { it.isNotEmpty() }
            ?: error("Table $this does not have a primary key")

    /** 主键列（只有一个时才不为空） */
    val primaryKeySingleOrNull get() = primaryKeys.singleOrNull()

    /** 主键列（非只有一个时抛出异常） */
    val primaryKey
        get() = primaryKeys.singleOrNull()
            ?: error("Table $this has more than one primary keys, use primaryKeys instead")

    open val insertColumns get() = emptyList<Column<E, *>>()
    open val updateColumns get() = emptyList<Column<E, *>>()

    fun <C : Any> registerColumn(column: Column<E, C>) {
        columnsMapMutable[column.name] = column
    }

    fun <C : Any> registerColumn(name: String, sqlType: SqlType<C>) =
        Column<E, C>(name, sqlType, this).also(this::registerColumn)

    override fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>) = also {
        appendTable(this@Table)
        alias?.also { append(' ').appendRef(it) }
    }

    override fun createEntity() = if (createEntity != null) {
        createEntity.invoke()
    } else {
        throw UnsupportedOperationException("Entity creation method of table $this is not specified")
    }

    override fun toString() = listOfNotNull(category, schema, tableName).joinToString(".")
}
