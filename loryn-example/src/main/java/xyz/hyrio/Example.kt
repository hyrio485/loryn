package xyz.hyrio

import top.loryn.database.Database
import top.loryn.expression.eq
import top.loryn.schema.Table
import top.loryn.statement.select
import top.loryn.support.int
import top.loryn.utils.get

class A {
    var id: Int? = null
    var field1: Int? = null
    var field2: Int? = null
}

object TableA : Table("table_a") {
    val id = int("id").primaryKey()
    val field1 = int("field1")
    val field2 = int("field2")
}

fun main() {
    val database = Database.connect("jdbc:mysql://localhost:3306/loryn_test", "root", "root")
    database.select(TableA) {
        where { it.id eq 1 }
    }.list {
        Triple(it[TableA.id], it[TableA.field2], 9)
    }.also {
        println(it)
    }
}
