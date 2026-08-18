package top.loryn.dialect.mysql

import top.loryn.database.SqlBuilder
import top.loryn.expression.*
import top.loryn.support.*
import top.loryn.utils.SqlParamList
import java.time.LocalDateTime
import java.util.*

fun <T> SqlExpression<T>.`if`(condition: SqlExpression<Boolean>, elseBranch: SqlExpression<T>) =
    FunctionExpression("IF", sqlType, condition, this, elseBranch)

fun <T> SqlExpression<T>.ifNull(default: SqlExpression<T>) =
    FunctionExpression("IFNULL", sqlType, this, default)

fun <T> SqlExpression<T>.ifNull(default: T?) =
    ifNull(expr(default))

fun rowNumber(partitionBy: List<ColumnExpression<*>>, orderBy: List<OrderByExpression>) =
    WindowExpression(FunctionExpression("ROW_NUMBER", IntSqlType), partitionBy, orderBy)

fun rowNumber(partitionBy: ColumnExpression<*>, vararg orderBy: OrderByExpression) =
    WindowExpression(FunctionExpression("ROW_NUMBER", IntSqlType), listOf(partitionBy), orderBy.toList())

fun <T> firstValue(expr: SqlExpression<T>, partitionBy: List<ColumnExpression<*>>, orderBy: List<OrderByExpression>) =
    WindowExpression(FunctionExpression("FIRST_VALUE", expr.sqlType, expr), partitionBy, orderBy)

fun <T> firstValue(expr: SqlExpression<T>, partitionBy: ColumnExpression<*>, vararg orderBy: OrderByExpression) =
    WindowExpression(FunctionExpression("FIRST_VALUE", expr.sqlType, expr), listOf(partitionBy), orderBy.toList())

fun <T> lastValue(expr: SqlExpression<T>, partitionBy: List<ColumnExpression<*>>, orderBy: List<OrderByExpression>) =
    WindowExpression(FunctionExpression("LAST_VALUE", expr.sqlType, expr), partitionBy, orderBy)

fun <T> lastValue(expr: SqlExpression<T>, partitionBy: ColumnExpression<*>, vararg orderBy: OrderByExpression) =
    WindowExpression(FunctionExpression("LAST_VALUE", expr.sqlType, expr), listOf(partitionBy), orderBy.toList())

val NOW = FunctionExpression("NOW", JavaDateSqlType)
val CURRENT_TIMESTAMP = FunctionExpression("CURRENT_TIMESTAMP", JavaDateSqlType, addParenthesesWhenNoArgs = false)

fun dateSub(time: SqlExpression<Date>, interval: Int, unit: MysqlTimeUnit) =
    FunctionExpression(
        "DATE_SUB", JavaDateSqlType, time,
        object : SqlExpression<Nothing> {
            override val sqlType get() = sqlTypeNoNeed()

            override fun buildSql(builder: SqlBuilder, params: SqlParamList, ignoreAlias: Boolean) {
                builder.appendKeyword("INTERVAL").append(' ').append(interval).append(' ').appendKeyword(unit.name)
            }
        },
    )

fun dateFormat(date: SqlExpression<Date>, pattern: String) =
    FunctionExpression("DATE_FORMAT", StringSqlType, date, StringSqlType.expr(pattern))

fun fromUnixTime(unixTime: SqlExpression<Long>) =
    FunctionExpression("FROM_UNIXTIME", LocalDateTimeSqlType, unixTime)

fun date(date: SqlExpression<LocalDateTime>) =
    FunctionExpression("DATE", LocalDateSqlType, date)

fun <T> coalesce(vararg expressions: SqlExpression<T>) =
    FunctionExpression("COALESCE", expressions.first().sqlType, *expressions)

inline fun <reified N : Number> coalesce(expr: SqlExpression<N>, number: N) =
    FunctionExpression("COALESCE", expr.sqlType, expr, expr.sqlType.expr(number))
