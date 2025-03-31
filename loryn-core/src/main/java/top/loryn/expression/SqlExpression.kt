package top.loryn.expression

import top.loryn.database.SqlBuilder
import top.loryn.support.SqlType

/**
 * 对表达式的抽象。
 */
interface SqlExpression<T : Any> {
    val sqlType: SqlType<T> get() = throw UnsupportedOperationException("${javaClass.simpleName} does not have a SQL type.")

    fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>): SqlBuilder
}
