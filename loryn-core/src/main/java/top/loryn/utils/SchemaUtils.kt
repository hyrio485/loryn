package top.loryn.utils

import top.loryn.expression.SqlParam
import java.lang.Boolean as JavaLangBoolean
import java.lang.Byte as JavaLangByte
import java.lang.Character as JavaLangCharacter
import java.lang.Double as JavaLangDouble
import java.lang.Float as JavaLangFloat
import java.lang.Integer as JavaLangInteger
import java.lang.Long as JavaLangLong
import java.lang.Short as JavaLangShort

typealias SqlParamList = MutableList<SqlParam<*>>

val Class<*>.boxed: Class<out Any>
    get() {
        return if (isPrimitive) {
            when (name) {
                "boolean" -> JavaLangBoolean::class.java
                "char" -> JavaLangCharacter::class.java
                "byte" -> JavaLangByte::class.java
                "short" -> JavaLangShort::class.java
                "int" -> JavaLangInteger::class.java
                "float" -> JavaLangFloat::class.java
                "long" -> JavaLangLong::class.java
                "double" -> JavaLangDouble::class.java
                "void" -> Void::class.java
                else -> this
            }
        } else this
    }
