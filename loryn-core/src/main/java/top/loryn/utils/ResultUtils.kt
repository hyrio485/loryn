package top.loryn.utils

import java.sql.ResultSet

inline fun <T> List<T>.one(lazyMessage: () -> String?) = when {
    size <= 1 -> firstOrNull()
    else -> error(lazyMessage() ?: "Expected one element but was $size")
}

fun <T> List<T>.one() = one { null }

operator fun <T> ResultSet.invoke(block: ResultSet.() -> T?) = block()?.takeUnless { wasNull() }
