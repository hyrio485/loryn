package top.loryn.expression

import top.loryn.schema.Column
import top.loryn.support.*

// region comparison operators

infix fun <C : Any> SqlExpression<C>.eq(other: SqlExpression<C>) = infixExprBool<C>("=", other)
infix fun <C : Any> SqlExpression<C>.eq(value: C?) = eq(value.toParameter(sqlType))
infix fun <C : Any> SqlExpression<C>.gt(other: SqlExpression<C>) = infixExprBool<C>(">", other)
infix fun <C : Any> SqlExpression<C>.gt(value: C?) = gt(value.toParameter(sqlType))
infix fun <C : Any> SqlExpression<C>.lt(other: SqlExpression<C>) = infixExprBool<C>("<", other)
infix fun <C : Any> SqlExpression<C>.lt(value: C?) = lt(value.toParameter(sqlType))
infix fun <C : Any> SqlExpression<C>.neq(other: SqlExpression<C>) = infixExprBool<C>("!=", other)
infix fun <C : Any> SqlExpression<C>.neq(value: C?) = neq(value.toParameter(sqlType))
infix fun <C : Any> SqlExpression<C>.gte(other: SqlExpression<C>) = infixExprBool<C>(">=", other)
infix fun <C : Any> SqlExpression<C>.gte(value: C?) = gte(value.toParameter(sqlType))
infix fun <C : Any> SqlExpression<C>.lte(other: SqlExpression<C>) = infixExprBool<C>("<=", other)
infix fun <C : Any> SqlExpression<C>.lte(value: C?) = lte(value.toParameter(sqlType))

// endregion

// region arithmetic operators

operator fun SqlExpression<Int>.plus(other: SqlExpression<Int>) = infixExprInt<Int>("+", other)
operator fun SqlExpression<Int>.plus(value: Int) = plus(value.toParameter())
operator fun SqlExpression<Int>.minus(other: SqlExpression<Int>) = infixExprInt<Int>("-", other)
operator fun SqlExpression<Int>.minus(value: Int) = minus(value.toParameter())
operator fun SqlExpression<Int>.times(other: SqlExpression<Int>) = infixExprInt<Int>("*", other)
operator fun SqlExpression<Int>.times(value: Int) = times(value.toParameter())
operator fun SqlExpression<Int>.div(other: SqlExpression<Int>) = infixExprInt<Int>("/", other)
operator fun SqlExpression<Int>.div(value: Int) = div(value.toParameter())

// endregion

// region logical operators

infix fun SqlExpression<Boolean>.and(other: SqlExpression<Boolean>) =
    infixExprBool<Boolean>("AND", other, addParentheses = true)

infix fun SqlExpression<Boolean>.or(other: SqlExpression<Boolean>) =
    infixExprBool<Boolean>("OR", other, addParentheses = true)

fun SqlExpression<Boolean>.andIf(condition: Boolean, other: SqlExpression<Boolean>) =
    if (condition) this and other else this

fun SqlExpression<Boolean>.orIf(condition: Boolean, other: SqlExpression<Boolean>) =
    if (condition) this or other else this

fun <A> SqlExpression<Boolean>.andIfNotNull(param: A?, other: SqlExpression<Boolean>) =
    if (param != null) this and other else this

fun <A> SqlExpression<Boolean>.orIfNotNull(param: A?, other: SqlExpression<Boolean>) =
    if (param != null) this or other else this

inline fun <A> SqlExpression<Boolean>.andIfNotNull(param: A?, getOther: (A) -> SqlExpression<Boolean>) =
    if (param != null) this and getOther(param) else this

inline fun <A> SqlExpression<Boolean>.orIfNotNull(param: A?, getOther: (A) -> SqlExpression<Boolean>) =
    if (param != null) this or getOther(param) else this

operator fun SqlExpression<Boolean>.not() =
    UnaryExpression<Boolean, Boolean>("NOT", this, BooleanSqlType, addParentheses = true)

// endregion

// region in operator

infix fun <E, C : Any> Column<E, C>.`in`(values: Iterable<C>) = InExpression(this, list = values.map { expr(it) })
infix fun SqlExpression<*>.`in`(list: List<SqlExpression<*>>) = InExpression(this, list = list)
infix fun SqlExpression<*>.`in`(select: SelectExpression<*>) = InExpression(this, select = select)
infix fun SqlExpression<*>.notIn(list: List<SqlExpression<*>>) = InExpression(this, list = list, not = true)
infix fun SqlExpression<*>.notIn(select: SelectExpression<*>) = InExpression(this, select = select, not = true)
infix fun Tuple.`in`(list: List<Tuple>) = InExpression(this, list = list)
infix fun Tuple.notIn(list: List<Tuple>) = InExpression(this, list = list, not = true)

// endregion

// region other operators

infix fun SqlExpression<String>.like(other: SqlExpression<String>) = infixExprBool<String>("LIKE", other)
infix fun SqlExpression<String>.like(value: String) = like(value.toParameter())

fun <E, C : Any> Column<E, C>.isNull() =
    infixExprBool<C>("IS", NullSqlExpression<C>())

fun <E, C : Any> Column<E, C>.isNotNull() =
    infixExprBool<C>(listOf("IS", "NOT"), NullSqlExpression<C>())

infix fun <E, C : Any> Column<E, C>.eqNullable(value: C?) =
    if (value == null) isNull() else eq(value)

fun <T : Any> T?.toParameter(sqlType: SqlType<T>) = ParameterExpression(this, sqlType)
fun Int?.toParameter() = toParameter(IntSqlType)
fun String?.toParameter() = toParameter(StringSqlType)
fun Boolean?.toParameter() = toParameter(BooleanSqlType)

fun <E, C : Any> SqlExpression<C>.toColumn() = ColumnExpression.wrap<E, C>(this)
fun <E, C : Any> ColumnExpression<E, C>.toOrderBy(type: OrderByType) = OrderByExpression(this, type)

fun <E, C : Any> ColumnExpression<E, C>.toOrderBy(ascending: Boolean = true) =
    toOrderBy(if (ascending) OrderByType.ASC else OrderByType.DESC)

// endregion

// region BinaryExpression utils

private fun <C : Any, R : Any> SqlExpression<C>.infixExpr(
    operators: List<String>,
    other: SqlExpression<C>,
    sqlType: SqlType<R>,
    addParentheses: Boolean = false,
) = InfixExpression<C, C, R>(operators, this, other, sqlType, addParentheses)

private fun <C : Any> SqlExpression<C>.infixExprBool(
    operators: List<String>,
    other: SqlExpression<C>,
    addParentheses: Boolean = false,
) = infixExpr<C, Boolean>(operators, other, BooleanSqlType, addParentheses)

private fun <C : Any> SqlExpression<C>.infixExprBool(
    operator: String,
    other: SqlExpression<C>,
    addParentheses: Boolean = false,
) = infixExprBool<C>(listOf(operator), other, addParentheses)

private fun <C : Any> SqlExpression<C>.infixExprInt(
    operators: List<String>,
    other: SqlExpression<C>,
    addParentheses: Boolean = false,
) = infixExpr<C, Int>(operators, other, IntSqlType, addParentheses)

private fun <C : Any> SqlExpression<C>.infixExprInt(
    operator: String,
    other: SqlExpression<C>,
    addParentheses: Boolean = false,
) = infixExprInt<C>(listOf(operator), other, addParentheses)

// endregion
