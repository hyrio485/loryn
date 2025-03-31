package top.loryn.expression

import top.loryn.support.SqlType
import java.sql.PreparedStatement

data class SqlParam<T : Any>(val value: T?, val sqlType: SqlType<T>) {
    fun setParameter(statement: PreparedStatement, index: Int) {
        sqlType.setParameter(statement, index, value)
    }

    override fun toString() = "$value(${sqlType.javaClassName})"
}
