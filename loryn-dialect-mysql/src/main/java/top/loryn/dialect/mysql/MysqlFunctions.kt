package top.loryn.dialect.mysql

import top.loryn.expression.*
import top.loryn.schema.Column
import top.loryn.support.IntSqlType

fun <T> SqlExpression<T>.`if`(condition: SqlExpression<Boolean>, elseBranch: SqlExpression<T>) =
    FunctionExpression("IF", sqlType, condition, this, elseBranch)

fun <T> SqlExpression<T>.ifNull(default: SqlExpression<T>) =
    FunctionExpression("IFNULL", sqlType, this, default)

fun <T> SqlExpression<T>.ifNull(default: T?) =
    ifNull(expr(default))

fun <T> Column<T>.max() =
    FunctionExpression("MAX", sqlType, this)

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
