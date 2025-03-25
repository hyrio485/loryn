package top.loryn.expression

import top.loryn.database.SqlBuilder

interface SqlExpression<T : Any> {
    val sqlType: SqlType<T> get() = throw UnsupportedOperationException()

    fun SqlBuilder.generateSql(params: MutableList<SqlParam<*>>): SqlBuilder
}
