package top.loryn.utils

fun <T> List<T>.one() = when {
    size <= 1 -> firstOrNull()
    else -> throw IllegalArgumentException("Expected one element but was $size")
}
