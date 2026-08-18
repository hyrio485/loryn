package top.loryn.expression

import top.loryn.schema.Column

fun <T> Column<T>.max() = FunctionExpression("MAX", sqlType, this)
fun <T> Column<T>.min() = FunctionExpression("MIN", sqlType, this)
fun <T> Column<T>.sum() = FunctionExpression("SUM", sqlType, this)
