package top.loryn.expression

import top.loryn.schema.Column

private fun <E, C : Any> SqlExpression<C>.binBool(operator: String, other: SqlExpression<C>) =
    BinaryExpression<E, C, C, Boolean>(operator, this, other, BooleanSqlType, addParentheses = false)

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
