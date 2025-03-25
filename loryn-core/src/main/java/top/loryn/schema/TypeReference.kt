package top.loryn.schema

import java.lang.reflect.ParameterizedType

abstract class TypeReference<T> {
    val referencedClass by lazy {
        fun doFind(cls: Class<*>): Class<*> {
            val genericSuperclass = cls.genericSuperclass
            if (genericSuperclass is Class<*>) {
                return if (genericSuperclass != TypeReference::class.java) {
                    doFind(genericSuperclass.superclass)
                } else { // direct subclass of TypeReference, e.g. TypeReference<Nothing>
                    Void::class.java
                }
            }
            return (genericSuperclass as ParameterizedType).actualTypeArguments[0] as? Class<*>
                ?: throw IllegalStateException("The referenced type of class $javaClass is not a class")
        }

        doFind(javaClass)
    }
}

inline fun <reified T> typeRef() = object : TypeReference<T>() {}

inline fun <reified T> typeOf() = typeRef<T>().referencedClass
