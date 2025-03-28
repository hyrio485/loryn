package top.loryn.expression

import top.loryn.schema.Column

private fun <E, C : Any> SqlExpression<C>.binBool(operator: String, other: SqlExpression<C>) =
    BinaryExpression<E, C, C, Boolean>(operator, this, other, BooleanSqlType, addParentheses = false)

private fun <E, C : Any> SqlExpression<C>.binBool(operators: List<String>, other: SqlExpression<C>) =
    BinaryExpression<E, C, C, Boolean>(operators, this, other, BooleanSqlType, addParentheses = false)

infix fun <E, C : Any> SqlExpression<C>.eq(other: SqlExpression<C>) = binBool<E, C>("=", other)
infix fun <E, C : Any> Column<E, C>.eq(value: C?) = eq<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.gt(other: SqlExpression<C>) = binBool<E, C>(">", other)
infix fun <E, C : Any> Column<E, C>.gt(value: C?) = gt<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.lt(other: SqlExpression<C>) = binBool<E, C>("<", other)
infix fun <E, C : Any> Column<E, C>.lt(value: C?) = lt<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.neq(other: SqlExpression<C>) = binBool<E, C>("!=", other)
infix fun <E, C : Any> Column<E, C>.neq(value: C?) = neq<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.gte(other: SqlExpression<C>) = binBool<E, C>(">=", other)
infix fun <E, C : Any> Column<E, C>.gte(value: C?) = gte<E, C>(expr(value))
infix fun <E, C : Any> SqlExpression<C>.lte(other: SqlExpression<C>) = binBool<E, C>("<=", other)
infix fun <E, C : Any> Column<E, C>.lte(value: C?) = lte<E, C>(expr(value))

infix fun <E> SqlExpression<String>.like(other: SqlExpression<String>) = binBool<E, String>("LIKE", other)
infix fun <E> Column<E, String>.like(value: String) = like<E>(expr(value))

fun <E, C : Any> Column<E, C>.isNull() =
    binBool<E, C>("IS", NullSqlExpression<E, C>())

fun <E, C : Any> Column<E, C>.isNotNull() =
    binBool<E, C>(listOf("IS", "NOT"), NullSqlExpression<E, C>())

infix fun <E, C : Any> Column<E, C>.eqNullable(value: C?) =
    if (value == null) isNull() else eq<E, C>(value)

private fun <E> SqlExpression<Int>.binInt(operator: String, other: SqlExpression<Int>) =
    BinaryExpression<E, Int, Int, Int>(operator, this, other, IntSqlType, addParentheses = false)

operator fun <E> SqlExpression<Int>.plus(other: SqlExpression<Int>) = binInt<E>("+", other)
operator fun <E> Column<E, Int>.plus(value: Int) = plus<E>(expr(value))
operator fun <E> SqlExpression<Int>.minus(other: SqlExpression<Int>) = binInt<E>("-", other)
operator fun <E> Column<E, Int>.minus(value: Int) = minus<E>(expr(value))
operator fun <E> SqlExpression<Int>.times(other: SqlExpression<Int>) = binInt<E>("*", other)
operator fun <E> Column<E, Int>.times(value: Int) = times<E>(expr(value))
operator fun <E> SqlExpression<Int>.div(other: SqlExpression<Int>) = binInt<E>("/", other)
operator fun <E> Column<E, Int>.div(value: Int) = div<E>(expr(value))

infix fun <E> SqlExpression<Boolean>.and(other: SqlExpression<Boolean>) =
    BinaryExpression<E, Boolean, Boolean, Boolean>("AND", this, other, BooleanSqlType)

infix fun <E> SqlExpression<Boolean>.or(other: SqlExpression<Boolean>) =
    BinaryExpression<E, Boolean, Boolean, Boolean>("OR", this, other, BooleanSqlType)

operator fun <E> SqlExpression<Boolean>.not() =
    UnaryExpression<E, Boolean, Boolean>("NOT", this, BooleanSqlType)

// in

infix fun <E, C : Any> Column<E, C>.`in`(values: Iterable<C>) = InExpression<E>(this, values.map { expr(it) })
infix fun <E> SqlExpression<*>.`in`(list: List<SqlExpression<*>>) = InExpression<E>(this, list)
infix fun <E> SqlExpression<*>.notIn(list: List<SqlExpression<*>>) = InExpression<E>(this, list, not = true)
infix fun <E> Tuple.`in`(list: List<Tuple>) = InExpression<E>(this, list)
infix fun <E> Tuple.notIn(list: List<Tuple>) = InExpression<E>(this, list, not = true)
