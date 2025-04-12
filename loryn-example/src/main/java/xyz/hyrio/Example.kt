package xyz.hyrio

import top.loryn.database.Database
import top.loryn.expression.BindableColumnExpression
import top.loryn.expression.toSqlParam
import top.loryn.schema.BindableTable
import top.loryn.statement.bindableDql
import top.loryn.support.IntSqlType

class A {
    var id: Int? = null
    var field1: Int? = null
    var field2: Int? = null

    override fun toString(): String {
        return "A(id=$id, field1=$field1, field2=$field2)"
    }
}

object TableA : BindableTable<A>("table_a", ::A) {
    val id = column("id", IntSqlType, A::id, primaryKey = true)
    val field1 = column("field1", IntSqlType, A::field1, notNull = true)
    val field2 = column("field2", IntSqlType, A::field2, notNull = true)
}

fun main() {
    val database = Database.connect("jdbc:mysql://localhost:3306/loryn_test", "root", "root")

    //    database.selectBindable(TableA) {
    //        where { it.id eq 1 }
    //    }.list {
    //        Triple(it[TableA.id], it[TableA.field2], 9)
    //    }.also {
    //        println(it)
    //    }

    val list = database.bindableDql(
        "SELECT * FROM table_a WHERE id = ?;",
        1.toSqlParam(),
        createEntity = ::A,
        columns = listOf(
            BindableColumnExpression<A, Int>("id", IntSqlType, A::id)
        )
    ).list()
    println(list)
}
