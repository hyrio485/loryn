package top.loryn.expression

import top.loryn.database.SqlBuilder

/**
 * 对表达式的抽象。
 */
interface SqlExpression<T : Any> {
    val sqlType: SqlType<T> get() = throw UnsupportedOperationException()

    fun SqlBuilder.appendSql(params: MutableList<SqlParam<*>>): SqlBuilder
}
