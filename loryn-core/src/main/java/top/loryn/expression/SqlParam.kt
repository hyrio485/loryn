package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType
import top.loryn.utils.SqlParamList
import java.sql.PreparedStatement

data class SqlParam<T>(val value: T?, override val sqlType: SqlType<T>) : SqlExpression<T> {
    fun setParameter(statement: PreparedStatement, index: Int) {
        sqlType.setParameter(statement, index, value)
    }

    override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
        builder.append("?")
        params += this@SqlParam
    }

    override fun toString() = "$value(${sqlType.javaClassName})"
}
