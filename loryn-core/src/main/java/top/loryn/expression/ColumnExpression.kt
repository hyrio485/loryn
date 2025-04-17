package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.support.WithAlias
import top.loryn.support.WithAlias.Companion.getAliasOrNull
import top.loryn.utils.SqlParamList
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1

interface ColumnExpression<T> : SqlExpression<T> {
    val name: String?

    companion object {
        operator fun <T> invoke(
            name: String,
            sqlType: SqlType<T>,
        ) = object : ColumnExpression<T> {
            override val name = name
            override val sqlType = sqlType

            override fun buildSql(builder: SqlBuilder, params: SqlParamList) =
                throw UnsupportedOperationException("Easy creation of ColumnExpression can only be used in ResultSet's column mapping.")
        }

        fun <T> wrap(expression: SqlExpression<T>, alias: String? = null): ColumnExpression<T> =
            object : ColumnExpression<T>, WithAlias {
                override val name = null
                override val sqlType = expression.sqlType

                override val alias = alias ?: expression.getAliasOrNull()
                override val original = expression
            }
    }

    fun getValue(resultSet: ResultSet) = sqlType.getResult(resultSet, (getAliasOrNull() ?: name)!!)

    fun <E> bind(getter: (E.() -> T?)? = null, setter: (E.(T?) -> Unit)? = null): BindableColumnExpression<E, T> =
        object : BindableColumnExpression<E, T>, WithAlias {
            private val this0 = this@ColumnExpression

            override val name = this0.name
            override val sqlType = this0.sqlType
            override val getter = getter
            override val setter = setter

            override val alias = this0.getAliasOrNull()
            override val original = this0
        }

    fun <E> bind(property: KMutableProperty1<E, T?>) =
        bind<E>({ property.get(this) }, { property.set(this, it) })

    fun aliased(alias: String): ColumnExpression<T> =
        object : ColumnExpression<T>, WithAlias {
            private val this0 = this@ColumnExpression

            override val name = this0.name
            override val sqlType = this0.sqlType

            override val alias = alias
            override val original = this0
        }
}
