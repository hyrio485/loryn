package top.loryn.expression

import top.loryn.schema.BooleanSqlType
import top.loryn.schema.Column

private fun <T : Any> SqlExpression<T>.cmp(operator: String, other: SqlExpression<T>) =
    BinaryExpression<T, T, Boolean>(operator, this, other, BooleanSqlType, addParentheses = false)

infix fun <T : Any> SqlExpression<T>.eq(other: SqlExpression<T>) = cmp("=", other)
infix fun <T : Any> SqlExpression<T>.gt(other: SqlExpression<T>) = cmp(">", other)
infix fun <T : Any> SqlExpression<T>.lt(other: SqlExpression<T>) = cmp("<", other)
infix fun <T : Any> SqlExpression<T>.neq(other: SqlExpression<T>) = cmp("!=", other)
infix fun <T : Any> SqlExpression<T>.gte(other: SqlExpression<T>) = cmp(">=", other)
infix fun <T : Any> SqlExpression<T>.lte(other: SqlExpression<T>) = cmp("<=", other)

private fun <T : Any> Column<T>.expr(value: T?) = ParameterExpression(value, sqlType)

infix fun <T : Any> Column<T>.eq(value: T?) = eq(expr(value))
infix fun <T : Any> Column<T>.gt(value: T?) = gt(expr(value))
infix fun <T : Any> Column<T>.lt(value: T?) = lt(expr(value))
infix fun <T : Any> Column<T>.neq(value: T?) = neq(expr(value))
infix fun <T : Any> Column<T>.gte(value: T?) = gte(expr(value))
infix fun <T : Any> Column<T>.lte(value: T?) = lte(expr(value))

infix fun SqlExpression<Boolean>.and(other: SqlExpression<Boolean>) =
    BinaryExpression<Boolean, Boolean, Boolean>("AND", this, other, BooleanSqlType)

infix fun SqlExpression<Boolean>.or(other: SqlExpression<Boolean>) =
    BinaryExpression<Boolean, Boolean, Boolean>("OR", this, other, BooleanSqlType)

operator fun SqlExpression<Boolean>.not() =
    UnaryExpression<Boolean, Boolean>("NOT", this, BooleanSqlType)
