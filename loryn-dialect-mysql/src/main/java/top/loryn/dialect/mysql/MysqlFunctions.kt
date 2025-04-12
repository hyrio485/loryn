package top.loryn.dialect.mysql

import top.loryn.expression.*
import top.loryn.schema.Column
import top.loryn.support.IntSqlType

fun <T> SqlExpression<T>.`if`(condition: SqlExpression<Boolean>, elseBranch: SqlExpression<T>) =
    FunctionExpression("IF", sqlType, condition, this, elseBranch)

fun <T> SqlExpression<T>.ifnull(default: SqlExpression<T>) =
    FunctionExpression("IFNULL", sqlType, this, default)

fun <T> SqlExpression<T>.ifnull(default: T?) =
    ifnull(default.toSqlParam(sqlType))

fun <C> Column<C>.max() =
    FunctionExpression("MAX", sqlType, this)

fun rowNumber(partitionBy: List<ColumnExpression<*>>, orderBy: List<OrderByExpression>) =
    WindowExpression(FunctionExpression("ROW_NUMBER", IntSqlType), partitionBy, orderBy)
