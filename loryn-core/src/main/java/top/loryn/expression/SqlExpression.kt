package top.loryn.expression

import top.loryn.support.SqlAppender
import top.loryn.support.SqlType

/**
 * 对表达式的抽象。
 */
interface SqlExpression<T> : SqlAppender {
    val sqlType: SqlType<T>

    fun expr(value: T?) = SqlParam<T>(value, sqlType)
}
