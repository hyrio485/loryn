package top.loryn.expression

import top.loryn.schema.Column
import top.loryn.support.*

// comparison operators

infix fun <C : Any> SqlExpression<C>.eq(other: SqlExpression<C>) = infixExprBool<C>("=", other)
infix fun <E, C : Any> Column<E, C>.eq(value: C?) = eq<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.gt(other: SqlExpression<C>) = infixExprBool<C>(">", other)
infix fun <E, C : Any> Column<E, C>.gt(value: C?) = gt<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.lt(other: SqlExpression<C>) = infixExprBool<C>("<", other)
infix fun <E, C : Any> Column<E, C>.lt(value: C?) = lt<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.neq(other: SqlExpression<C>) = infixExprBool<C>("!=", other)
infix fun <E, C : Any> Column<E, C>.neq(value: C?) = neq<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.gte(other: SqlExpression<C>) = infixExprBool<C>(">=", other)
infix fun <E, C : Any> Column<E, C>.gte(value: C?) = gte<C>(expr(value))
infix fun <C : Any> SqlExpression<C>.lte(other: SqlExpression<C>) = infixExprBool<C>("<=", other)
infix fun <E, C : Any> Column<E, C>.lte(value: C?) = lte<C>(expr(value))

// arithmetic operators

operator fun SqlExpression<Int>.plus(other: SqlExpression<Int>) = infixExprInt<Int>("+", other)
operator fun <E> Column<E, Int>.plus(value: Int) = plus(expr(value))
operator fun SqlExpression<Int>.minus(other: SqlExpression<Int>) = infixExprInt<Int>("-", other)
operator fun <E> Column<E, Int>.minus(value: Int) = minus(expr(value))
operator fun SqlExpression<Int>.times(other: SqlExpression<Int>) = infixExprInt<Int>("*", other)
operator fun <E> Column<E, Int>.times(value: Int) = times(expr(value))
operator fun SqlExpression<Int>.div(other: SqlExpression<Int>) = infixExprInt<Int>("/", other)
operator fun <E> Column<E, Int>.div(value: Int) = div(expr(value))

// logical operators

infix fun SqlExpression<Boolean>.and(other: SqlExpression<Boolean>) =
    infixExprBool<Boolean>("AND", other)

infix fun SqlExpression<Boolean>.or(other: SqlExpression<Boolean>) =
    infixExprBool<Boolean>("OR", other)

fun SqlExpression<Boolean>.andIf(condition: Boolean, other: SqlExpression<Boolean>) =
    if (condition) this and other else this

fun SqlExpression<Boolean>.orIf(condition: Boolean, other: SqlExpression<Boolean>) =
    if (condition) this or other else this

operator fun SqlExpression<Boolean>.not() =
    UnaryExpression<Boolean, Boolean>("NOT", this, BooleanSqlType)

// in operator

infix fun <E, C : Any> Column<E, C>.`in`(values: Iterable<C>) = InExpression(this, list = values.map { expr(it) })
infix fun SqlExpression<*>.`in`(list: List<SqlExpression<*>>) = InExpression(this, list = list)
infix fun SqlExpression<*>.`in`(select: SelectExpression<*>) = InExpression(this, select = select)
infix fun SqlExpression<*>.notIn(list: List<SqlExpression<*>>) = InExpression(this, list = list, not = true)
infix fun SqlExpression<*>.notIn(select: SelectExpression<*>) = InExpression(this, select = select, not = true)
infix fun Tuple.`in`(list: List<Tuple>) = InExpression(this, list = list)
infix fun Tuple.notIn(list: List<Tuple>) = InExpression(this, list = list, not = true)

// other operators

infix fun SqlExpression<String>.like(other: SqlExpression<String>) = infixExprBool<String>("LIKE", other)
infix fun <E> Column<E, String>.like(value: String) = like(expr(value))

fun <E, C : Any> Column<E, C>.isNull() =
    infixExprBool<C>("IS", NullSqlExpression<C>())

fun <E, C : Any> Column<E, C>.isNotNull() =
    infixExprBool<C>(listOf("IS", "NOT"), NullSqlExpression<C>())

infix fun <E, C : Any> Column<E, C>.eqNullable(value: C?) =
    if (value == null) isNull() else eq<E, C>(value)

fun Int.toParameterExpression() = ParameterExpression(this, IntSqlType)
fun String.toParameterExpression() = ParameterExpression(this, StringSqlType)
fun Boolean.toParameterExpression() = ParameterExpression(this, BooleanSqlType)

// BinaryExpression utils

private fun <C : Any, R : Any> SqlExpression<C>.infixExpr(
    operators: List<String>,
    other: SqlExpression<C>,
    sqlType: SqlType<R>,
) = InfixExpression<C, C, R>(operators, this, other, sqlType)

private fun <C : Any> SqlExpression<C>.infixExprBool(operators: List<String>, other: SqlExpression<C>) =
    infixExpr<C, Boolean>(operators, other, BooleanSqlType)

private fun <C : Any> SqlExpression<C>.infixExprBool(operator: String, other: SqlExpression<C>) =
    infixExprBool<C>(listOf(operator), other)

private fun <C : Any> SqlExpression<C>.infixExprInt(operators: List<String>, other: SqlExpression<C>) =
    infixExpr<C, Int>(operators, other, IntSqlType)

private fun <C : Any> SqlExpression<C>.infixExprInt(operator: String, other: SqlExpression<C>) =
    infixExprInt<C>(listOf(operator), other)
