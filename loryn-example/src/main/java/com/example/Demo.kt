package com.example

import org.junit.jupiter.api.Test
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
import top.loryn.database.Database
import top.loryn.dialect.mysql.MysqlSqlDialect
import top.loryn.dialect.mysql.insertOrUpdate
import top.loryn.expression.*
import top.loryn.schema.JoinQuerySource
import top.loryn.statement.*
import top.loryn.support.IntSqlType
import top.loryn.support.LocalDateTimeSqlType
import top.loryn.support.StringSqlType
import top.loryn.utils.get
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import kotlin.concurrent.thread

class Test {
    private val database = Database.connect("jdbc:mysql://localhost:3306/loryn_test", "root", "root")

    @Test
    fun `get started`() {
        val userPairs1 = database.select(Users).list {
            it[Users.id] to it[Users.userName]
        }
        println(userPairs1)

        val userPairs2 = database.select(Users) {
            where((it.id eq 101) or (it.userName like "%白%"))
        }.list {
            it[Users.id] to it[Users.userName]
        }
        println(userPairs2)

        val users1 = database.selectBindable(BindableUsers).list()
        println(users1)
    }

    @Test
    fun `use single database connection`() {
        // App 启动时，建立连接
        val conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/loryn_test", "root", "root")

        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                // 进程退出时，关闭连接
                conn.close()
            }
        )

        val database = Database.connect {
            object : Connection by conn {
                override fun close() {
                    // 重写 close 方法，保持连接不关闭
                }
            }
        }
    }

    @Test
    fun `use transaction`() {
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

    @Test
    fun `exception translator`() {
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
            dialect = MysqlSqlDialect()
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
                where(it.id eq 101)
            }
        }
    }

    @Test
    fun `mysql insert or update`() {
        database.insertOrUpdate(Users) {
            assign(it.id, 101)
            assign(it.userName, "abc")
            assign(it.createdAt, LocalDateTime.now())
            set(it.userName, "abc")
        }.execute()
        database.insertOrUpdate(BindableUsers, UserPo().apply {
            id = 101
            userName = "abc"
            createdAt = LocalDateTime.now()
        })
    }

    @Test
    fun `use native sql dql`() {
        // val idColumn = ColumnExpression("id", IntSqlType)
        // val userNameColumn = ColumnExpression("username", StringSqlType)
        database
            .dql(
                "SELECT * FROM users WHERE id = ?",
                101.toSqlParam(),
                columns = listOf(ColumnExpression("id", IntSqlType))
            )
            .list {
                it[Users.id] to it[Users.userName]
                // it[idColumn] to it[userNameColumn]
            }
            .forEach { println(it) }
    }

    @Test
    fun `use native sql dql bindable`() {
        val idColumn = BindableColumnExpression("id", IntSqlType, UserPo::id)
        val userNameColumn = BindableColumnExpression("username", StringSqlType, UserPo::userName)
        database
            .dqlBindable(
                "SELECT * FROM users WHERE id = ?",
                101.toSqlParam(),
                createEntity = ::UserPo,
                columns = listOf(idColumn, userNameColumn)
            )
            .list()
            .forEach { println(it) }
    }

    @Test
    fun `use native sql dml`() {
        val effects = database.dml(
            "INSERT INTO users (id, username, created_at) VALUES (?, ?, ?)",
            101.toSqlParam(),
            "abc".toSqlParam(),
            LocalDateTime.now().toSqlParam(LocalDateTimeSqlType),
        ).execute()
        println(effects)
    }

    @Test
    fun `use native sql jdbc`() {
        database.useConnection { conn ->
            conn.prepareStatement("SELECT * FROM users WHERE id = ?").use { statement ->
                statement.setInt(1, 101)
                statement.executeQuery().use {
                    while (it.next()) {
                        val id = it.getInt("id")
                        val userName = it.getString("username")
                        println("id: $id, username: $userName")
                    }
                }
            }
        }
    }

    @Test
    fun `column aliased`() {
        val i = Users.id.aliased("i")
        database.select(Users) {
            column(i)
            where(i gte 102)
        }.list { it[i] }.also(::println)
    }

    @Test
    fun `query from joined table`() {
        val u = Users.aliased("u")
        val o = Orders.aliased("o")
        var userIdColumn = u[Users.id]
        var userNameColumn = u[Users.userName]
        var productIdColumn = o[Orders.productId]
        database.select(u.join(o, joinType = JoinQuerySource.JoinType.LEFT, on = userIdColumn eq o[Orders.userId])) {
            column(userNameColumn, productIdColumn)
            where(userIdColumn gte 102)
        }.list { it[userNameColumn] to it[productIdColumn] }.also(::println)
    }

    @Test
    fun `query and map to entities`() {
        database.selectBindable(BindableOrders) {
            where(it.userId `in` Users.select {
                column(it.id)
                where(it.id gte 102)
            })
        }.list().also(::println)
    }

    @Test
    fun `dml statements`() {
        val user = UserPo().apply { /* 省略字段赋值操作 */ }
        database.insert(BindableUsers, user, useGeneratedKeys = true)

        val effects1 = database.delete(Users) { where(it.id eq 101) }
        val effects2 = database.delete(BindableUsers, user)
        val effects3 = database.update(Users) {
            set(it.userName, "abc")
            where(it.id eq 101)
        }
        val effects4 = database.update(BindableUsers, user)

        val effects5 = database.deleteLogically(BindableUsers, user)
        val effects6 = database.updateOptimistic(BindableUsers, user)
    }
}
