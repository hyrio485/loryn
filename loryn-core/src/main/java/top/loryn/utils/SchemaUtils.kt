package top.loryn.utils

import top.loryn.expression.SqlParam

typealias SqlParamList = MutableList<SqlParam<*>>

val Class<*>.boxed
    get() = when (this) {
        Boolean::class.javaPrimitiveType -> Boolean::class.java
        Byte::class.javaPrimitiveType -> Byte::class.java
        Char::class.javaPrimitiveType -> Char::class.java
        Short::class.javaPrimitiveType -> Short::class.java
        Int::class.javaPrimitiveType -> Int::class.java
        Long::class.javaPrimitiveType -> Long::class.java
        Float::class.javaPrimitiveType -> Float::class.java
        Double::class.javaPrimitiveType -> Double::class.java
        else -> this
    }
