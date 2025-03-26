package top.loryn.expression

import top.loryn.schema.Column

private fun <E, C : Any> SqlExpression<C>.cmp(operator: String, other: SqlExpression<C>) =
    BinaryExpression<E, C, C, Boolean>(operator, this, other, BooleanSqlType, addParentheses = false)

infix fun <E, C : Any> SqlExpression<C>.eq(other: SqlExpression<C>) = cmp<E, C>("=", other)
infix fun <E, C : Any> SqlExpression<C>.gt(other: SqlExpression<C>) = cmp<E, C>(">", other)
infix fun <E, C : Any> SqlExpression<C>.lt(other: SqlExpression<C>) = cmp<E, C>("<", other)
infix fun <E, C : Any> SqlExpression<C>.neq(other: SqlExpression<C>) = cmp<E, C>("!=", other)
infix fun <E, C : Any> SqlExpression<C>.gte(other: SqlExpression<C>) = cmp<E, C>(">=", other)
infix fun <E, C : Any> SqlExpression<C>.lte(other: SqlExpression<C>) = cmp<E, C>("<=", other)

infix fun <E, C : Any> Column<E, C>.eq(value: C?) = eq<E, C>(expr(value))
infix fun <E, C : Any> Column<E, C>.gt(value: C?) = gt<E, C>(expr(value))
infix fun <E, C : Any> Column<E, C>.lt(value: C?) = lt<E, C>(expr(value))
infix fun <E, C : Any> Column<E, C>.neq(value: C?) = neq<E, C>(expr(value))
infix fun <E, C : Any> Column<E, C>.gte(value: C?) = gte<E, C>(expr(value))
infix fun <E, C : Any> Column<E, C>.lte(value: C?) = lte<E, C>(expr(value))

infix fun <E> SqlExpression<Boolean>.and(other: SqlExpression<Boolean>) =
    BinaryExpression<E, Boolean, Boolean, Boolean>("AND", this, other, BooleanSqlType)

infix fun <E> SqlExpression<Boolean>.or(other: SqlExpression<Boolean>) =
    BinaryExpression<E, Boolean, Boolean, Boolean>("OR", this, other, BooleanSqlType)

operator fun <E> SqlExpression<Boolean>.not() =
    UnaryExpression<E, Boolean, Boolean>("NOT", this, BooleanSqlType)
