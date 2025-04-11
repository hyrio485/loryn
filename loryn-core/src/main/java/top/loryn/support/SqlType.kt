package top.loryn.support

import java.sql.JDBCType
import java.sql.PreparedStatement
import java.sql.ResultSet

abstract class SqlType<T>(val jdbcType: JDBCType, val clazz: Class<T>) {
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

    abstract fun doGetResult(rs: ResultSet, index: Int): T?

    open fun getResult(rs: ResultSet, index: Int): T? {
        return doGetResult(rs, index).takeUnless { rs.wasNull() }
    }

    open fun getResult(rs: ResultSet, label: String): T? {
        return getResult(rs, rs.findColumn(label))
    }

    fun <R> transform(clazz: Class<R>, fromBasedTypeToNew: (T) -> R, fromNewTypeToBased: (R) -> T) =
        object : SqlType<R>(jdbcType, clazz) {
            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: R) {
                this@SqlType.doSetParameter(ps, index, fromNewTypeToBased(parameter))
            }

            override fun doGetResult(rs: ResultSet, index: Int): R? =
                this@SqlType.doGetResult(rs, index)?.let(fromBasedTypeToNew)
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
