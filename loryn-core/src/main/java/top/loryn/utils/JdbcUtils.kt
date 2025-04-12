package top.loryn.utils

import top.loryn.expression.ColumnExpression
import java.sql.ResultSet

fun <R> ResultSet.mapEachRow(block: ResultSet.() -> R): List<R> = use {
    mutableListOf<R>().apply {
        while (next()) {
            add(block())
        }
    }
}

operator fun <T> ResultSet.get(column: ColumnExpression<T>) = column.getValue(this)
