package top.loryn.expression

import top.loryn.schema.Column
import top.loryn.support.BooleanSqlType
import top.loryn.support.IntSqlType
import top.loryn.support.SqlType
import top.loryn.support.Tuple

// comparison operators

infix fun <C : Any> SqlExpression<C>.eq(other: SqlExpression<C>) = binExprBool<C>("=", other)
infix fun <E, C : Any> Column<E, C>.eq(value: C?) = eq<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.gt(other: SqlExpression<C>) = binExprBool<C>(">", other)
infix fun <E, C : Any> Column<E, C>.gt(value: C?) = gt<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.lt(other: SqlExpression<C>) = binExprBool<C>("<", other)
infix fun <E, C : Any> Column<E, C>.lt(value: C?) = lt<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.neq(other: SqlExpression<C>) = binExprBool<C>("!=", other)
infix fun <E, C : Any> Column<E, C>.neq(value: C?) = neq<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.gte(other: SqlExpression<C>) = binExprBool<C>(">=", other)
infix fun <E, C : Any> Column<E, C>.gte(value: C?) = gte<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.lte(other: SqlExpression<C>) = binExprBool<C>("<=", other)
infix fun <E, C : Any> Column<E, C>.lte(value: C?) = lte<C>(expr(value))

// arithmetic operators

operator fun SqlExpression<Int>.plus(other: SqlExpression<Int>) = binExprInt<Int>("+", other)
operator fun <E> Column<E, Int>.plus(value: Int) = plus(expr(value))
operator fun SqlExpression<Int>.minus(other: SqlExpression<Int>) = binExprInt<Int>("-", other)
operator fun <E> Column<E, Int>.minus(value: Int) = minus(expr(value))
operator fun SqlExpression<Int>.times(other: SqlExpression<Int>) = binExprInt<Int>("*", other)
operator fun <E> Column<E, Int>.times(value: Int) = times(expr(value))
operator fun SqlExpression<Int>.div(other: SqlExpression<Int>) = binExprInt<Int>("/", other)
operator fun <E> Column<E, Int>.div(value: Int) = div(expr(value))

// logical operators

infix fun SqlExpression<Boolean>.and(other: SqlExpression<Boolean>) =
    binExprBool<Boolean>("AND", other)

infix fun SqlExpression<Boolean>.or(other: SqlExpression<Boolean>) =
    binExprBool<Boolean>("OR", other)

operator fun SqlExpression<Boolean>.not() =
    UnaryExpression<Boolean, Boolean>("NOT", this, BooleanSqlType)

// in operator

infix fun <E, C : Any> Column<E, C>.`in`(values: Iterable<C>) = InExpression(this, values.map { expr(it) })
infix fun <E> SqlExpression<*>.`in`(list: List<SqlExpression<*>>) = InExpression(this, list)
infix fun <E> SqlExpression<*>.notIn(list: List<SqlExpression<*>>) = InExpression(this, list, not = true)
infix fun <E> Tuple.`in`(list: List<Tuple>) = InExpression(this, list)
infix fun <E> Tuple.notIn(list: List<Tuple>) = InExpression(this, list, not = true)

// other operators

infix fun SqlExpression<String>.like(other: SqlExpression<String>) = binExprBool<String>("LIKE", other)
infix fun <E> Column<E, String>.like(value: String) = like(expr(value))

fun <E, C : Any> Column<E, C>.isNull() =
    binExprBool<C>("IS", NullSqlExpression<C>())

fun <E, C : Any> Column<E, C>.isNotNull() =
    binExprBool<C>(listOf("IS", "NOT"), NullSqlExpression<C>())

infix fun <E, C : Any> Column<E, C>.eqNullable(value: C?) =
    if (value == null) isNull() else eq<E, C>(value)

// BinaryExpression utils

private fun <C : Any, R : Any> SqlExpression<C>.binExpr(
    operators: List<String>,
    other: SqlExpression<C>,
    sqlType: SqlType<R>,
) = BinaryExpression<C, C, R>(operators, this, other, sqlType)

private fun <C : Any> SqlExpression<C>.binExprBool(operators: List<String>, other: SqlExpression<C>) =
    binExpr<C, Boolean>(operators, other, BooleanSqlType)

private fun <C : Any> SqlExpression<C>.binExprBool(operator: String, other: SqlExpression<C>) =
    binExprBool<C>(listOf(operator), other)

private fun <C : Any> SqlExpression<C>.binExprInt(operators: List<String>, other: SqlExpression<C>) =
    binExpr<C, Int>(operators, other, IntSqlType)

private fun <C : Any> SqlExpression<C>.binExprInt(operator: String, other: SqlExpression<C>) =
    binExprInt<C>(listOf(operator), other)
