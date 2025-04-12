package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.WithAlias
import top.loryn.support.WithAlias.Companion.getAliasOrNull
import top.loryn.utils.SqlParamList
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1

interface ColumnExpression<T> : SqlExpression<T> {
    val name: String?

    companion object {
        fun <T> wrap(expression: SqlExpression<T>) = object : ColumnExpression<T> {
            override val name = null
            override val sqlType = expression.sqlType

            override fun SqlBuilder.appendSql(params: SqlParamList) =
                append(expression, params)
        }
    }

    fun expr(value: T?) = SqlParam<T>(value, sqlType)

    fun getValue(resultSet: ResultSet) = sqlType.getResult(resultSet, (getAliasOrNull() ?: name)!!)

    fun <E> bind(getter: (E.() -> T?)? = null, setter: (E.(T?) -> Unit)? = null): BindableColumnExpression<E, T> =
        object : BindableColumnExpression<E, T> {
            private val this0 = this@ColumnExpression

            override val name = this0.name
            override val sqlType = this0.sqlType
            override val getter = getter
            override val setter = setter

            override fun SqlBuilder.appendSql(params: SqlParamList) = with(this0) { appendSql(params) }
        }

    fun <E> bind(property: KMutableProperty1<E, T?>) =
        bind<E>({ property.get(this) }, { property.set(this, it) })

    fun aliased(alias: String): ColumnExpression<T> =
        object : ColumnExpression<T>, WithAlias {
            private val this0 = this@ColumnExpression

            override val name = this0.name
            override val sqlType = this0.sqlType
            override val alias = alias

            override fun SqlBuilder.appendSql(params: SqlParamList) = with(this0) { appendSql(params) }
        }
}
