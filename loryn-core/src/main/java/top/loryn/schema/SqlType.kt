package top.loryn.schema

import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet

abstract class SqlType<T : Any>(val jdbcType: JDBCType, val clazz: Class<T>) {
    val jdbcTypeCode: Int = jdbcType.vendorTypeNumber
    val javaClassName: String = clazz.simpleName

    protected abstract fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T)

    open fun setParameter(ps: PreparedStatement, index: Int, parameter: T?) {
        if (parameter == null) {
            ps.setNull(index, jdbcTypeCode)
        } else {
            doSetParameter(ps, index, parameter)
        }
    }

    protected abstract fun doGetResult(rs: ResultSet, index: Int): T

    open fun getResult(rs: ResultSet, index: Int) =
        rs.takeUnless { it.wasNull() }?.let { doGetResult(it, index) }

    protected open fun doGetResult(rs: ResultSet, columnLabel: String) =
        doGetResult(rs, rs.findColumn(columnLabel))

    open fun getResult(rs: ResultSet, columnLabel: String) =
        rs.takeUnless { it.wasNull() }?.let { doGetResult(it, columnLabel) }

    open fun <R : Any> transform(clazz: Class<R>, from: (T) -> R, to: (R) -> T) =
        object : SqlType<R>(jdbcType, clazz) {
            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: R) {
                this@SqlType.doSetParameter(ps, index, to(parameter))
            }

            override fun doGetResult(rs: ResultSet, index: Int) =
                from(this@SqlType.doGetResult(rs, index))
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SqlType<*>

        if (jdbcType != other.jdbcType) return false
        if (clazz != other.clazz) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jdbcType.hashCode()
        result = 31 * result + clazz.hashCode()
        return result
    }

    override fun toString(): String {
        return "SqlType(jdbcType=$jdbcType, clazz=$clazz)"
    }
}
