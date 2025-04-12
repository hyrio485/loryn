package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.WithAlias.Companion.getAliasOrNull
import top.loryn.utils.SqlParamList
import java.sql.ResultSet

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
}
