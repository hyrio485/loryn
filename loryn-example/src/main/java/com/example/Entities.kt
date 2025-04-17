package com.example

import top.loryn.schema.BindableTable
import top.loryn.schema.Table
import top.loryn.support.DoubleSqlType
import top.loryn.support.IntSqlType
import top.loryn.support.LocalDateTimeSqlType
import top.loryn.support.StringSqlType
import java.time.LocalDateTime

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

    override val insertColumns = listOf(userName, createdAt, lastLogin)
    override val updateColumns = listOf(userName, createdAt, lastLogin)
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
