package top.loryn.dialect.mysql

import top.loryn.expression.*
import top.loryn.schema.Column
import top.loryn.support.IntSqlType

fun <T : Any> SqlExpression<T>.ifnull(default: SqlExpression<T>) =
    FunctionExpression("IFNULL", sqlType, this, default)

fun <T : Any> SqlExpression<T>.ifnull(default: T?) =
    ifnull(default.toParameter(sqlType))

fun <E, C : Any> Column<E, C>.max() =
    FunctionExpression("MAX", sqlType, this)

fun rowNumber(partitionBy: List<ColumnExpression<*, *>>, orderBy: List<OrderByExpression<*>>) =
    WindowExpression(FunctionExpression("ROW_NUMBER", IntSqlType), partitionBy, orderBy)
