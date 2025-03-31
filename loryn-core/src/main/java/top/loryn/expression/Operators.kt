package top.loryn.expression

import top.loryn.schema.Column
import top.loryn.support.BooleanSqlType
import top.loryn.support.IntSqlType
import top.loryn.support.SqlType
import top.loryn.support.Tuple

// comparison operators

infix fun <E, C : Any> SqlExpression<C>.eq(other: SqlExpression<C>) = binExprBool<E, C>("=", other)
infix fun <E, C : Any> Column<E, C>.eq(value: C?) = eq<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.gt(other: SqlExpression<C>) = binExprBool<E, C>(">", other)
infix fun <E, C : Any> Column<E, C>.gt(value: C?) = gt<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.lt(other: SqlExpression<C>) = binExprBool<E, C>("<", other)
infix fun <E, C : Any> Column<E, C>.lt(value: C?) = lt<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.neq(other: SqlExpression<C>) = binExprBool<E, C>("!=", other)
infix fun <E, C : Any> Column<E, C>.neq(value: C?) = neq<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.gte(other: SqlExpression<C>) = binExprBool<E, C>(">=", other)
infix fun <E, C : Any> Column<E, C>.gte(value: C?) = gte<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.lte(other: SqlExpression<C>) = binExprBool<E, C>("<=", other)
infix fun <E, C : Any> Column<E, C>.lte(value: C?) = lte<E, C>(expr(value))

// arithmetic operators

operator fun <E> SqlExpression<Int>.plus(other: SqlExpression<Int>) = binExprInt<E, Int>("+", other)
operator fun <E> Column<E, Int>.plus(value: Int) = plus<E>(expr(value))
operator fun <E> SqlExpression<Int>.minus(other: SqlExpression<Int>) = binExprInt<E, Int>("-", other)
operator fun <E> Column<E, Int>.minus(value: Int) = minus<E>(expr(value))
operator fun <E> SqlExpression<Int>.times(other: SqlExpression<Int>) = binExprInt<E, Int>("*", other)
operator fun <E> Column<E, Int>.times(value: Int) = times<E>(expr(value))
operator fun <E> SqlExpression<Int>.div(other: SqlExpression<Int>) = binExprInt<E, Int>("/", other)
operator fun <E> Column<E, Int>.div(value: Int) = div<E>(expr(value))

// logical operators

infix fun <E> SqlExpression<Boolean>.and(other: SqlExpression<Boolean>) =
    binExprBool<E, Boolean>("AND", other)

infix fun <E> SqlExpression<Boolean>.or(other: SqlExpression<Boolean>) =
    binExprBool<E, Boolean>("OR", other)

operator fun <E> SqlExpression<Boolean>.not() =
    UnaryExpression<E, Boolean, Boolean>("NOT", this, BooleanSqlType)

// in operator

infix fun <E, C : Any> Column<E, C>.`in`(values: Iterable<C>) = InExpression<E>(this, values.map { expr(it) })
infix fun <E> SqlExpression<*>.`in`(list: List<SqlExpression<*>>) = InExpression<E>(this, list)
infix fun <E> SqlExpression<*>.notIn(list: List<SqlExpression<*>>) = InExpression<E>(this, list, not = true)
infix fun <E> Tuple.`in`(list: List<Tuple>) = InExpression<E>(this, list)
infix fun <E> Tuple.notIn(list: List<Tuple>) = InExpression<E>(this, list, not = true)

// other operators

infix fun <E> SqlExpression<String>.like(other: SqlExpression<String>) = binExprBool<E, String>("LIKE", other)
infix fun <E> Column<E, String>.like(value: String) = like<E>(expr(value))

fun <E, C : Any> Column<E, C>.isNull() =
    binExprBool<E, C>("IS", NullSqlExpression<E, C>())

fun <E, C : Any> Column<E, C>.isNotNull() =
    binExprBool<E, C>(listOf("IS", "NOT"), NullSqlExpression<E, C>())

infix fun <E, C : Any> Column<E, C>.eqNullable(value: C?) =
    if (value == null) isNull() else eq<E, C>(value)

// BinaryExpression utils

private fun <E, C : Any, R : Any> SqlExpression<C>.binExpr(
    operators: List<String>,
    other: SqlExpression<C>,
    sqlType: SqlType<R>,
) = BinaryExpression<E, C, C, R>(operators, this, other, sqlType)

private fun <E, C : Any> SqlExpression<C>.binExprBool(operators: List<String>, other: SqlExpression<C>) =
    binExpr<E, C, Boolean>(operators, other, BooleanSqlType)

private fun <E, C : Any> SqlExpression<C>.binExprBool(operator: String, other: SqlExpression<C>) =
    binExprBool<E, C>(listOf(operator), other)

private fun <E, C : Any> SqlExpression<C>.binExprInt(operators: List<String>, other: SqlExpression<C>) =
    binExpr<E, C, Int>(operators, other, IntSqlType)

private fun <E, C : Any> SqlExpression<C>.binExprInt(operator: String, other: SqlExpression<C>) =
    binExprBool<E, C>(listOf(operator), other)
