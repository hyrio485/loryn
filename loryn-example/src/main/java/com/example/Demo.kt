package com.example

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
import top.loryn.database.Database
import top.loryn.expression.eq
import top.loryn.expression.like
import top.loryn.expression.or
import top.loryn.schema.BindableTable
import top.loryn.schema.Table
import top.loryn.statement.insert
import top.loryn.statement.select
import top.loryn.statement.selectBindable
import top.loryn.statement.update
import top.loryn.support.DoubleSqlType
import top.loryn.support.IntSqlType
import top.loryn.support.LocalDateTimeSqlType
import top.loryn.support.StringSqlType
import top.loryn.utils.get
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import kotlin.concurrent.thread

object Users : Table("users") {
    val id = column("id", IntSqlType, primaryKey = true)
    val userName = column("username", StringSqlType, notNull = true)
    val createdAt = column("created_at", LocalDateTimeSqlType, notNull = true)
    val lastLogin = column("last_login", LocalDateTimeSqlType)
}

object Products : Table("products") {
    val id = column("id", IntSqlType, primaryKey = true)
    val productName = column("product_name", StringSqlType, notNull = true)
    val price = column("price", DoubleSqlType, notNull = true)
    val createdAt = column("created_at", IntSqlType, notNull = true)
}

object Orders : Table("orders") {
    val id = column("id", IntSqlType, primaryKey = true)
    val userId = column("user_id", IntSqlType, notNull = true)
    val productId = column("product_id", IntSqlType, notNull = true)
    val quantity = column("quantity", IntSqlType, notNull = true)
    val orderTime = column("order_time", LocalDateTimeSqlType, notNull = true)
    val status = column("status", StringSqlType, notNull = true)
}

class UserPo {
    var id: Int? = null
    var userName: String? = null
    var createdAt: LocalDateTime? = null
    var lastLogin: LocalDateTime? = null

    override fun toString() =
        "UserPo(id=$id, userName=$userName, createdAt=$createdAt, lastLogin=$lastLogin)"
}

class ProductPo {
    var id: Int? = null
    var productName: String? = null
    var price: Double? = null
    var stock: Int? = null

    override fun toString() =
        "ProductPo(id=$id, productName=$productName, price=$price, stock=$stock)"
}

class OrderPo {
    var id: Int? = null
    var userId: Int? = null
    var productId: Int? = null
    var quantity: Int? = null
    var orderTime: LocalDateTime? = null
    var status: String? = null

    override fun toString() =
        "OrderPo(id=$id, userId=$userId, productId=$productId, quantity=$quantity, orderTime=$orderTime, status=$status)"
}

object BindableUsers : BindableTable<UserPo>("users", ::UserPo) {
    val id = column("id", IntSqlType, UserPo::id, primaryKey = true)
    val userName = column("username", StringSqlType, UserPo::userName, notNull = true)
    val createdAt = column("created_at", LocalDateTimeSqlType, UserPo::createdAt, notNull = true)
    val lastLogin = column("last_login", LocalDateTimeSqlType, UserPo::lastLogin)
}

object BindableProducts : BindableTable<ProductPo>("products", ::ProductPo) {
    val id = column("id", IntSqlType, ProductPo::id, primaryKey = true)
    val productName = column("product_name", StringSqlType, ProductPo::productName, notNull = true)
    val price = column("price", DoubleSqlType, ProductPo::price, notNull = true)
    val stock = column("stock", IntSqlType, ProductPo::stock, notNull = true)
}

object BindableOrders : BindableTable<OrderPo>("orders", ::OrderPo) {
    val id = column("id", IntSqlType, OrderPo::id, primaryKey = true)
    val userId = column("user_id", IntSqlType, OrderPo::userId, notNull = true)
    val productId = column("product_id", IntSqlType, OrderPo::productId, notNull = true)
    val quantity = column("quantity", IntSqlType, OrderPo::quantity, notNull = true)
    val orderTime = column("order_time", LocalDateTimeSqlType, OrderPo::orderTime, notNull = true)
    val status = column("status", StringSqlType, OrderPo::status, notNull = true)
}

fun main1() {
    val database = Database.connect("jdbc:mysql://localhost:3306/loryn_test", "root", "root")

    val userPairs1 = database.select(Users).list {
        it[Users.id] to it[Users.userName]
    }
    println(userPairs1)

    val userPairs2 = database.select(Users) {
        where { (it.id eq 101) or (it.userName like "%白%") }
    }.list {
        it[Users.id] to it[Users.userName]
    }
    println(userPairs2)

    val users1 = database.selectBindable(BindableUsers).list()
    println(users1)

    // App 启动时，建立连接
    val conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/loryn_test", "root", "root")

    Runtime.getRuntime().addShutdownHook(
        thread(start = false) {
            // 进程退出时，关闭连接
            conn.close()
        }
    )

    val database1 = Database.connect {
        object : Connection by conn {
            override fun close() {
                // 重写 close 方法，保持连接不关闭
            }
        }
    }
}

fun main2() {
    val database = Database.connect("jdbc:mysql://localhost:3306/loryn_test", "root", "root")

    class DummyException : Exception()
    try {
        database.useTransaction {
            database.insert(Users) {
                assign(it.userName, "aaa")
                assign(it.createdAt, LocalDateTime.now())
            }.execute()
            println("----> " + database.select(Users).count())
            throw DummyException()
        }
    } catch (_: DummyException) {
        println("----> " + database.select(Users).count())
    }
}

fun main3() {
    val dataSource = SingleConnectionDataSource(
        "jdbc:mysql://localhost:3306/loryn_test", "root", "root", true
    )
    val translator = SQLErrorCodeSQLExceptionTranslator(dataSource)
    val database = Database.connect(
        dataSource,
        exceptionTranslator = {
            val (sqlException, sql) = it
            translator.translate("[Loryn] ${it.message}", sql, sqlException)
        },
    )
    try {
        database.insert(Users) {
            assign(it.id, 101)
            assign(it.userName, "abc")
            assign(it.createdAt, LocalDateTime.now())
        }.execute()
    } catch (_: DuplicateKeyException) {
        database.update(Users) {
            set(it.userName, "abc")
            where { it.id eq 101 }
        }
    }
}

fun main() {
    //    main1()
    //    main2()
    main3()
}
