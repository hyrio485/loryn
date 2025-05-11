package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.support.WithAlias
import top.loryn.utils.SqlParamList
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

            override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) =
                throw UnsupportedOperationException("Easy creation of BindableColumnExpression can only be used in ResultSet's column mapping.")
        }

        operator fun <E, T> invoke(
            name: String,
            sqlType: SqlType<T>,
            property: KMutableProperty1<E, T?>,
        ) = invoke<E, T>(
            name,
            sqlType,
            getter = { property.get(this) },
            setter = { property.set(this, it) },
        )
    }

    // 某些情况可能只需要 getter 或 setter，如使用原始 SQL 时，因此要指定为可空类型
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
    fun <T1> getValueAndTransform(
        entity: E,
        block: (BindableColumnExpression<E, T>, T?) -> SqlExpression<T1>,
    ) = block(this, getValue(entity))

    override fun aliased(alias: String): BindableColumnExpression<E, T> =
        object : BindableColumnExpression<E, T>, WithAlias {
            private val this0 = this@BindableColumnExpression

            override val name = this0.name
            override val sqlType = this0.sqlType
            override val getter = this0.getter
            override val setter = this0.setter

            override val alias = alias
            override val original = this0
        }
}
