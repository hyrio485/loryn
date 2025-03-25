package top.loryn.expression

import top.loryn.schema.SqlType
import java.sql.PreparedStatement

data class SqlParam<T : Any>(val value: T?, val sqlType: SqlType<T>) {
    fun setParameter(statement: PreparedStatement, index: Int) {
        sqlType.setParameter(statement, index, value)
    }

    override fun toString() = "$value(${sqlType.javaClassName})"
}

data class SqlAndParams(val sql: String, val params: List<SqlParam<*>>) {
    constructor(sql: String, vararg params: SqlParam<*>) : this(sql, params.toList())
}
