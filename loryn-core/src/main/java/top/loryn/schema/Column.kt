package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.ColumnExpression
import top.loryn.expression.ParameterExpression
import top.loryn.expression.SqlParam
import top.loryn.expression.SqlType
import java.sql.ResultSet

/**
 * 数据表的一列。
 *
 * @param C 列的数据类型。
 */
class Column<E, C : Any>(
    val table: Table<E>,
    val name: String,
    sqlType: SqlType<C>,
    label: String? = null,
    val primaryKey: Boolean = false,
    val notNull: Boolean = false,
    setter: (E.(C?) -> Unit)? = null,
    val getter: (E.() -> C?)? = null,
) : ColumnExpression<E, C>(sqlType, label, setter) {
    private fun copy(
        table: Table<E> = this.table,
        name: String = this.name,
        sqlType: SqlType<C> = this.sqlType,
        alias: String? = this.alias,
        primaryKey: Boolean = this.primaryKey,
        notNull: Boolean = this.notNull,
        setter: (E.(C?) -> Unit)? = this.setter,
        getter: (E.() -> C?)? = this.getter,
    ) = Column(table, name, sqlType, alias, primaryKey, notNull, setter, getter)

    private fun Column<E, C>.registerColumn() = also { table.registerColumn(it) }
    fun primaryKey(primaryKey: Boolean = true) = copy(primaryKey = primaryKey).registerColumn()
    fun notNull(notNull: Boolean = true) = copy(notNull = notNull).registerColumn()
    fun setter(setter: E.(C?) -> Unit) = copy(setter = setter).registerColumn()
    fun getter(getter: E.() -> C?) = copy(getter = getter).registerColumn()

    fun expr(value: C?) = ParameterExpression<E, C>(value, sqlType)

    fun getValue(entity: E) =
        (getter ?: throw IllegalArgumentException("Column $name does not have a getter")).invoke(entity)

    fun getValueExpr(entity: E) = expr(getValue(entity))

    fun setValue(entity: E, index: Int, resultSet: ResultSet) {
        (setter ?: throw IllegalArgumentException("Column $name does not have a setter"))
            .invoke(entity, sqlType.getResult(resultSet, index + 1))
    }

    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        table.alias?.also { appendRef(it).append('.') }
        appendRef(name)
    }

    override fun toString() = "${table.alias?.let { "$it." }.orEmpty()}$name${alias?.let { "($it)" }.orEmpty()}"
}
