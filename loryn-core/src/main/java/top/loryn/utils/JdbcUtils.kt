package top.loryn.utils

import top.loryn.schema.Column
import java.sql.ResultSet

fun <R> ResultSet.mapEachRow(block: ResultSet.() -> R): List<R> = use {
    mutableListOf<R>().apply {
        while (next()) {
            add(block())
        }
    }
}

operator fun <C> ResultSet.get(column: Column<C>) = column.sqlType.getResult(this, column.name)
