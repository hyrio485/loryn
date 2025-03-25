package top.loryn.database

import java.sql.ResultSet

fun <R> ResultSet.mapEachRow(block: ResultSet.() -> R): List<R> = use {
    mutableListOf<R>().apply {
        while (next()) {
            add(block())
        }
    }
}
