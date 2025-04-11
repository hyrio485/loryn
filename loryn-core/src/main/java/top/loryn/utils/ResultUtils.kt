package top.loryn.utils

import top.loryn.expression.ColumnExpression
import java.sql.ResultSet

inline fun <T> List<T>.one(lazyMessage: () -> String?) = when {
    size <= 1 -> firstOrNull()
    else -> throw IllegalArgumentException(lazyMessage() ?: "Expected one element but was $size")
}

fun <T> List<T>.one() = one { null }

operator fun <E, C : Any> ResultSet.get(column: ColumnExpression<E, C>) = column.getValue(this)
