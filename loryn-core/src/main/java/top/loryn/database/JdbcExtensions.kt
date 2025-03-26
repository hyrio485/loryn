package top.loryn.database

import top.loryn.schema.Column
import java.sql.ResultSet

fun <R> ResultSet.mapEachRow(block: ResultSet.() -> R): List<R> = use {
    mutableListOf<R>().apply {
        while (next()) {
            add(block())
        }
    }
}

operator fun <E, C : Any> ResultSet.get(column: Column<E, C>): C? =
    column.sqlType.getResult(this, column.name)
