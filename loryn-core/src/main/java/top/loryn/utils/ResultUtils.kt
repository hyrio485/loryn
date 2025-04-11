package top.loryn.utils

inline fun <T> List<T>.one(lazyMessage: () -> String?) = when {
    size <= 1 -> firstOrNull()
    else -> throw IllegalArgumentException(lazyMessage() ?: "Expected one element but was $size")
}

fun <T> List<T>.one() = one { null }
