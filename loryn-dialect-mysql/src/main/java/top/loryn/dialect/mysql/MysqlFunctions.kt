package top.loryn.dialect.mysql

import top.loryn.expression.FunctionExpression
import top.loryn.expression.SqlExpression
import top.loryn.expression.toParameter
import top.loryn.schema.Column

fun <T : Any> SqlExpression<T>.ifnull(default: SqlExpression<T>) =
    FunctionExpression("IFNULL", sqlType, this, default)

fun <T : Any> SqlExpression<T>.ifnull(default: T?) =
    ifnull(default.toParameter(sqlType))

fun <E, C : Any> Column<E, C>.max() =
    FunctionExpression("MAX", sqlType, this)
