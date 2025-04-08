package top.loryn.schema

import top.loryn.database.SqlBuilder
import top.loryn.expression.*
import top.loryn.support.SqlType
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1

/**
 * 数据表的一列。
 *
 * @param C 列的数据类型。
 */
open class Column<E, C : Any>(
    val name: String,
    sqlType: SqlType<C>,
    val table: Table<E>? = null,
    alias: String? = null,
    val primaryKey: Boolean = false,
    val notNull: Boolean = false,
    setter: (E.(C?) -> Unit)? = null,
    val getter: (E.() -> C?)? = null,
) : ColumnExpression<E, C>(alias, name, sqlType, setter) {
    fun primaryKey(primaryKey: Boolean = true): Column<E, C> =
        DerivedColumn(this, primaryKey = primaryKey, notNull = true)

    fun notNull(notNull: Boolean = true): Column<E, C> = DerivedColumn(this, notNull = notNull)

    fun setter(setter: E.(C?) -> Unit): Column<E, C> = DerivedColumn(this, setter = setter)

    fun getter(getter: E.() -> C?): Column<E, C> = DerivedColumn(this, getter = getter)

    fun bind(property: KMutableProperty1<E, C?>): Column<E, C> =
        DerivedColumn(this, setter = { property.set(this, it) }, getter = { property.get(this) })

    fun aliased(alias: String): Column<E, C> = DerivedColumn(this, alias = alias)

    fun expr(value: C?) = ParameterExpression<C>(value, sqlType)

    fun getValue(entity: E) =
        (getter ?: throw IllegalArgumentException("Column $name does not have a getter")).invoke(entity)

    fun getValueExpr(entity: E) = expr(getValue(entity))

    // 主要为了解决泛型问题
    fun <T : Any> getValueAndTransform(entity: E, block: (Column<E, C>, C?) -> SqlExpression<T>) =
        block(this, getValue(entity))

    fun setValue(entity: E, index: Int, resultSet: ResultSet) {
        (setter ?: throw IllegalArgumentException("Column $name does not have a setter"))
            .invoke(entity, sqlType.getResult(resultSet, index + 1))
    }

    fun assignByValue(value: C?) = AssignmentExpression<E, C>(this, expr(value))
    fun assignByEntity(entity: E) = AssignmentExpression<E, C>(this, getValueExpr(entity))

    override fun SqlBuilder.appendSqlOriginal(params: MutableList<SqlParam<*>>) = also {
        table?.alias?.also { appendRef(it).append('.') }
        appendRef(name)
    }

    open val root = this

    override fun toString() =
        "${table?.alias?.let { "$it." }.orEmpty()}$name${this@Column.alias?.let { "($it)" }.orEmpty()}"
}
