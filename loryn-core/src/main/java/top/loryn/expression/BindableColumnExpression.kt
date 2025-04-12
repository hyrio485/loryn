package top.loryn.expression

import top.loryn.support.SqlType
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1

interface BindableColumnExpression<E, T> : ColumnExpression<T> {
    companion object {
        operator fun <E, T> invoke(
            name: String,
            sqlType: SqlType<T>,
            getter: (E.() -> T?)? = null,
            setter: (E.(T?) -> Unit)? = null,
        ) = object : BindableColumnExpression<E, T> {
            override val name = name
            override val sqlType = sqlType
            override val getter = getter
            override val setter = setter
        }

        operator fun <E, T> invoke(
            name: String,
            sqlType: SqlType<T>,
            property: KMutableProperty1<E, T?>,
        ) = invoke<E, T>(
            name,
            sqlType,
            getter = { property.get(this) },
            setter = { property.set(this, it) }
        )
    }

    val getter: (E.() -> T?)?
    val setter: (E.(T?) -> Unit)?

    val getterNotNull get() = getter ?: error("Column $this does not have a getter")
    val setterNotNull get() = setter ?: error("Column $this does not have a setter")

    fun getValue(entity: E) = getterNotNull(entity)

    fun getValueExpr(entity: E) = expr(getValue(entity))

    fun setValue(entity: E, index: Int, resultSet: ResultSet) {
        setterNotNull(entity, sqlType.getResult(resultSet, index + 1))
    }

    fun assignByValue(value: T?) = AssignmentExpression(this, expr(value))
    fun assignByEntity(entity: E) = AssignmentExpression(this, getValueExpr(entity))

    private inline fun doApplyValue(entity: E, getValue: () -> T?) {
        entity.setterNotNull(getValue())
    }

    fun applyValue(entity: E, index: Int, resultSet: ResultSet) {
        doApplyValue(entity) { sqlType.getResult(resultSet, index + 1) }
    }

    fun applyValue(entity: E, resultSet: ResultSet) {
        doApplyValue(entity) { getValue(resultSet) }
    }

    // 主要为了解决泛型问题
    fun <T1> getValueAndTransform(entity: E, block: (BindableColumnExpression<E, T>, T?) -> SqlExpression<T1>) =
        block(this, getValue(entity))
}
