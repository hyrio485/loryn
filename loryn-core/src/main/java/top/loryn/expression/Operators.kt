package top.loryn.expression

import top.loryn.expression.OrderByExpression.OrderByType
import top.loryn.support.*

// region comparison operators

infix fun <C> SqlExpression<C>.eq(other: SqlExpression<C>) = InfixExpression("=", this, other, BooleanSqlType)
infix fun <C> SqlExpression<C>.eq(value: C?) = eq(expr(value))
infix fun <C> SqlExpression<C>.gt(other: SqlExpression<C>) = InfixExpression(">", this, other, BooleanSqlType)
infix fun <C> SqlExpression<C>.gt(value: C?) = gt(expr(value))
infix fun <C> SqlExpression<C>.lt(other: SqlExpression<C>) = InfixExpression("<", this, other, BooleanSqlType)
infix fun <C> SqlExpression<C>.lt(value: C?) = lt(expr(value))
infix fun <C> SqlExpression<C>.neq(other: SqlExpression<C>) = InfixExpression("!=", this, other, BooleanSqlType)
infix fun <C> SqlExpression<C>.neq(value: C?) = neq(expr(value))
infix fun <C> SqlExpression<C>.gte(other: SqlExpression<C>) = InfixExpression(">=", this, other, BooleanSqlType)
infix fun <C> SqlExpression<C>.gte(value: C?) = gte(expr(value))
infix fun <C> SqlExpression<C>.lte(other: SqlExpression<C>) = InfixExpression("<=", this, other, BooleanSqlType)
infix fun <C> SqlExpression<C>.lte(value: C?) = lte(expr(value))

// endregion
// region arithmetic operators

operator fun SqlExpression<Int>.plus(other: SqlExpression<Int>) = InfixExpression("+", this, other, IntSqlType)
operator fun SqlExpression<Int>.plus(value: Int) = plus(expr(value))
operator fun SqlExpression<Int>.minus(other: SqlExpression<Int>) = InfixExpression("-", this, other, IntSqlType)
operator fun SqlExpression<Int>.minus(value: Int) = minus(expr(value))
operator fun SqlExpression<Int>.times(other: SqlExpression<Int>) = InfixExpression("*", this, other, IntSqlType)
operator fun SqlExpression<Int>.times(value: Int) = times(expr(value))
operator fun SqlExpression<Int>.div(other: SqlExpression<Int>) = InfixExpression("/", this, other, IntSqlType)
operator fun SqlExpression<Int>.div(value: Int) = div(expr(value))
operator fun SqlExpression<Int>.rem(other: SqlExpression<Int>) = InfixExpression("%", this, other, IntSqlType)
operator fun SqlExpression<Int>.rem(value: Int) = rem(expr(value))

operator fun SqlExpression<Int>.unaryPlus() =
    UnaryExpression<Int, Int>("+", this, IntSqlType, addParentheses = false, addWhiteSpace = false)

operator fun SqlExpression<Int>.unaryMinus() =
    UnaryExpression<Int, Int>("-", this, IntSqlType, addParentheses = false, addWhiteSpace = false)

// endregion
// region logical operators

infix fun SqlExpression<Boolean>.and(other: SqlExpression<Boolean>) =
    InfixExpression("AND", this, other, BooleanSqlType, addParentheses = true)

infix fun SqlExpression<Boolean>.or(other: SqlExpression<Boolean>) =
    InfixExpression("OR", this, other, BooleanSqlType, addParentheses = true)

fun SqlExpression<Boolean>.andIf(condition: Boolean, other: SqlExpression<Boolean>) =
    if (condition) this and other else this

fun SqlExpression<Boolean>.orIf(condition: Boolean, other: SqlExpression<Boolean>) =
    if (condition) this or other else this

fun <P> SqlExpression<Boolean>.andIfNotNull(param: P?, other: SqlExpression<Boolean>) =
    if (param != null) this and other else this

fun <P> SqlExpression<Boolean>.orIfNotNull(param: P?, other: SqlExpression<Boolean>) =
    if (param != null) this or other else this

inline fun <P> SqlExpression<Boolean>.andIfNotNull(param: P?, getOther: (P) -> SqlExpression<Boolean>) =
    if (param != null) this and getOther(param) else this

inline fun <P> SqlExpression<Boolean>.orIfNotNull(param: P?, getOther: (P) -> SqlExpression<Boolean>) =
    if (param != null) this or getOther(param) else this

fun andAll(vararg expressions: SqlExpression<Boolean>) =
    expressions.reduce { acc, expression -> acc and expression }

fun orAll(vararg expressions: SqlExpression<Boolean>) =
    expressions.reduce { acc, expression -> acc or expression }

operator fun SqlExpression<Boolean>.not() =
    UnaryExpression<Boolean, Boolean>("NOT", this, BooleanSqlType, addParentheses = false)

// endregion
// region in operator

infix fun <C> ColumnExpression<C>.`in`(values: Iterable<C>) =
    InExpression(this, list = values.map { expr(it) })

infix fun SqlExpression<*>.`in`(list: List<SqlExpression<*>>) = InExpression(this, list = list)
infix fun SqlExpression<*>.`in`(select: SelectExpression) = InExpression(this, select = select)
infix fun Tuple.`in`(list: List<Tuple>) = InExpression(this, list = list)

infix fun <C> ColumnExpression<C>.notIn(values: Iterable<C>) =
    InExpression(this, list = values.map { expr(it) }, not = true)

infix fun SqlExpression<*>.notIn(list: List<SqlExpression<*>>) = InExpression(this, list = list, not = true)
infix fun SqlExpression<*>.notIn(select: SelectExpression) = InExpression(this, select = select, not = true)
infix fun Tuple.notIn(list: List<Tuple>) = InExpression(this, list = list, not = true)

fun <T> SqlExpression<T>.between(start: SqlExpression<T>, end: SqlExpression<T>) =
    TernaryExpression("BETWEEN", "AND", this, start, end, BooleanSqlType)

fun <T> SqlExpression<T>.between(start: T, end: T) =
    between(expr(start), expr(end))

fun <T : Comparable<T>> SqlExpression<T>.between(range: ClosedRange<T>) =
    between(range.start, range.endInclusive)

// endregion
// region other operators

infix fun SqlExpression<String>.like(other: SqlExpression<String>) =
    InfixExpression("LIKE", this, other, BooleanSqlType)

infix fun SqlExpression<String>.like(value: String) =
    like(expr(value))

infix fun SqlExpression<String>.notLike(other: SqlExpression<String>) =
    InfixExpression(listOf("NOT", "LIKE"), this, other, BooleanSqlType)

fun <C> ColumnExpression<C>.isNull() =
    InfixExpression("IS", this, NullSqlExpression<C>(), BooleanSqlType)

fun <C> ColumnExpression<C>.isNotNull() =
    InfixExpression(listOf("IS", "NOT"), this, NullSqlExpression<C>(), BooleanSqlType)

infix fun <C> ColumnExpression<C>.eqNullable(value: C?) =
    if (value == null) isNull() else eq(value)

fun <T> SqlExpression<T>.count() =
    UnaryExpression<T, Int>("COUNT", this, IntSqlType, addParentheses = true)

fun <T> T?.toSqlParam(sqlType: SqlType<T>) = sqlType.expr(this)
fun Int?.toSqlParam() = toSqlParam(IntSqlType)
fun String?.toSqlParam() = toSqlParam(StringSqlType)
fun Boolean?.toSqlParam() = toSqlParam(BooleanSqlType)

// endregion
// region column related

fun <T> SqlExpression<T>.distinct(alias: String? = null) =
    UnaryExpression("DISTINCT", this, sqlType, false).asColumn(alias)

fun <T> SqlExpression<T>.asColumn(alias: String? = null) =
    ColumnExpression.wrap<T>(this, alias)

fun <T> SqlExpression<T>.toOrderBy(type: OrderByType) =
    OrderByExpression(asColumn(), type)

fun <C> ColumnExpression<C>.toOrderBy(type: OrderByType) =
    OrderByExpression(this, type)

fun <T> SqlExpression<T>.toOrderBy(ascending: Boolean = true) =
    OrderByExpression(asColumn(), ascending)

fun <C> ColumnExpression<C>.toOrderBy(ascending: Boolean = true) =
    OrderByExpression(this, ascending)

// endregion
