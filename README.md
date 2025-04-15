<h1>Loryn</h1>

[![Apache 2.0](https://img.shields.io/badge/license-Apache%202-red.svg)](LICENSE)
[![gitee star](https://gitee.com/hyrio485/loryn/badge/star.svg?theme=dark)](https://gitee.com/hyrio485/loryn/stargazers)
![github star](https://img.shields.io/github/stars/hyrio485/loryn.svg)

Loryn是为Kotlin量身定制的，基于JDBC的极简ORM框架，它提供了强类型的SQL DSL，旨在减少我们手动拼接SQL出错的可能。
同时，Loryn还提供了一个简单的对象关系映射（ORM）功能，允许我们将数据库表映射到Kotlin数据类，从而简化了数据访问和操作的过程。

Loryn基于Apache 2.0协议开源，欢迎大家使用和贡献代码。
如果你在使用过程中遇到问题或有任何建议，请随时在GitHub或Gitee上提交issue，我们会尽快回复和处理。

# 开始

## 特性

- **轻量级**：Loryn是一个轻量级的ORM框架，无需配置文件、注解等，无三方依赖，易于集成和使用。
- **强类型SQL DSL**：Loryn提供了一个强类型的SQL DSL，允许我们使用Kotlin的类型系统来构建SQL查询，避免了手动拼接SQL时可能出现的错误。
- **简单易用**：Loryn的API设计简单易用，使用起来非常方便。用户可以选择自动实体映射或操作原生ResultSet，在性能和易用性之间取得平衡。
- **支持多种数据库**：Loryn提供了方言接口，支持多种数据库，用户可以根据需要选择合适的方言。
- **支持事务**：Loryn支持事务管理，用户可以方便地进行事务操作。
- **支持连接池**：Loryn支持连接池，用户可以根据需要选择合适的连接池实现。

## 快速开始

添加依赖：

```xml

<dependency>
  <groupId>top.loryn</groupId>
  <artifactId>loryn-core</artifactId>
  <version>${loryn.version}</version>
</dependency>
```

假设有如下的表结构：

```sql
-- 用户表（单表操作主要对象）
CREATE TABLE users
(
    id         INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username   VARCHAR(50) NOT NULL COMMENT '用户名',
    created_at DATETIME    NOT NULL COMMENT '注册时间',
    last_login DATETIME COMMENT '最后登录时间'
) COMMENT '用户信息表';

-- 商品表（单表操作+一对多关联）
CREATE TABLE products
(
    id           INT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    product_name VARCHAR(100)   NOT NULL COMMENT '商品名称',
    price        DECIMAL(10, 2) NOT NULL COMMENT '销售价格',
    stock        INT            NOT NULL COMMENT '库存数量'
) COMMENT '商品信息表';

-- 订单表（多表关联枢纽）
CREATE TABLE orders
(
    id         INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    user_id    INT         NOT NULL COMMENT '关联用户ID',
    product_id INT         NOT NULL COMMENT '关联商品ID',
    quantity   INT         NOT NULL COMMENT '购买数量',
    order_time DATETIME    NOT NULL COMMENT '下单时间',
    status     VARCHAR(20) NOT NULL COMMENT '订单状态'
) COMMENT '订单记录表';

-- 模拟数据
INSERT INTO users (id, username, created_at, last_login)
VALUES (101, '林若曦', '2024-03-01 09:00:00', '2024-04-15 09:15:00'),
       (102, '沈墨白', '2024-02-15 14:30:00', '2024-04-14 18:20:00'),
       (103, '陆清歌', '2024-01-10 10:00:00', '2024-04-13 16:45:00');

INSERT INTO products (product_name, price, stock)
VALUES ('无线降噪耳机', 899.00, 50),
       ('智能手表Pro', 1299.00, 30),
       ('便携充电宝', 199.00, 100);

INSERT INTO orders (user_id, product_id, quantity, order_time, status)
VALUES (101, 201, 1, '2024-04-11 11:00:00', 'completed'),
       (101, 203, 2, '2024-04-13 15:45:00', 'cancelled'),
       (102, 201, 2, '2024-04-10 14:30:00', 'completed'),
       (102, 202, 1, '2024-04-12 09:15:00', 'completed'),
       (102, 203, 3, '2024-04-14 16:20:00', 'pending'),
       (103, 202, 1, '2024-04-09 17:30:00', 'completed');
```

首先需要用代码表述表结构：

```kotlin
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
    val stock = column("stock", IntSqlType, notNull = true)
}

object Orders : Table("orders") {
    val id = column("id", IntSqlType, primaryKey = true)
    val userId = column("user_id", IntSqlType, notNull = true)
    val productId = column("product_id", IntSqlType, notNull = true)
    val quantity = column("quantity", IntSqlType, notNull = true)
    val orderTime = column("order_time", LocalDateTimeSqlType, notNull = true)
    val status = column("status", StringSqlType, notNull = true)
}
```

用户可以使用如下代码连接到数据库并执行一个简单的查询：

```kotlin
fun main() {
    val database = Database.connect("jdbc:mysql://localhost:3306/loryn_test", "root", "root")
    val users = database.select(Users).list { resultSet ->
        resultSet[Users.id] to resultSet[Users.userName]
    }
    println(users) // [(101, 林若曦), (102, 沈墨白), (103, 陆清歌)]
}
```

在运行上述程序时，Loryn会生成SQL `SELECT * FROM users;` 以查询用户表中的所有记录。
`list`方法中定义了列映射的方式；Loryn重载了`ResultSet`的`get`方法，允许我们使用列名或列对象来获取列值。

## SQL DSL

上一节的示例程序获取了用户表的所有数据，而在实际开发中，我们通常需要根据条件来查询数据，如下所示：

```kotlin
val users1 = database.select(Users) {
    where { (it.id eq 101) or (it.userName like "%白%") }
}.list {
    it[Users.id] to it[Users.userName]
}
println(users1) // [(101, 林若曦), (102, 沈墨白)]
```

生成的SQL为：

```sql
SELECT *
FROM users
WHERE (id = ?)
   OR (username LIKE ?);
```

借助Kotlin的语法特性与Loryn的SQL DSL，我们可以轻松地写出兼具可读性和高性能的SQL查询。
与此同时，借助Kotlin的类型系统，Loryn的SQL DSL可以在编译时检查SQL语句的正确性，避免了运行时错误。

Loryn提供了丰富的DSL语法来支持各种DQL与DML操作，用法详见后续章节。

## 实体类与列绑定

实际项目中，多数情况我们需要把查询出结果集映射到实体类中。此时，我们可以对数据表定义的代码少加改造以实现实体类映射的功能。

首先我们需要定义实体类（Persistent Object）：

> 目前仅支持使用无餐构造器创建实体类；使用属性引用或使用getter/setter方法访问属性。

```kotlin
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
```

接下来我们需要改造之前的数据表定义，将`Table`改为`BindableTable`，并指定实体类的构造函数及属性引用或赋值方法：

```kotlin
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
```

完成以上改造后，我们可以方便地使用`selectBindable`方法查询数据并将结果映射到实体类中：

```kotlin
val users = database.selectBindable(BindableUsers).list()
```

同样，Loryn也支持基于实体类的DML操作，详见后续章节。

# 数据库连接相关

## 连接到数据库

使用`Database.connect()`方法可连接到数据库。Loryn重载了多次`connect()`方法以支持多种数据库连接方式。

### 手动管理连接

对于小型项目或存在特殊需求的项目而言，我们可能希望在程序运行的整个声明周期中仅存在一个连接对象，此时我们可以使用如下代码手动管理连接：

```kotlin
// 程序启动时，建立连接
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
```

在上述代码中，我们在程序启动时建立了一个连接，并注册了一个钩子函数，在进程退出时关闭连接。
在`Database.connect()`方法中，我们使用了一个匿名类实现了`Connection`接口，并重写了`close()`方法，保持连接不关闭，
这样在需要使用连接时，我们会创建一个不会关闭的实现了`Connection`接口的匿名对象以供后续的数据库操作使用。
这样，当Loryn需要获取连接时，调用这个闭包函数讲始终获取到同样一个连接对象，保证了连接的复用。

### 使用JDBC URL连接

```kotlin
val database = Database.connect(
    url = "jdbc:mysql://localhost:3306/loryn_test",
    user = "root",
    password = "root",
    driver = "com.mysql.cj.jdbc.Driver",
)
```

根据以上代码很容易理解`Database.connect()`方法的参数含义。
需要注意的是，Loryn在方法调用是会自动加载JDBC驱动，同时使用连接获取数据库元信息，之后立刻关闭连接。
使用此方法Loryn并不会在内存中保存连接对象，每次进行数据库操作时都会重新连接数据库，没有任何复用连接的行为。
众所周知，创建连接是一个开销很大的操作，因此此方法只建议在开发或测试阶段临时使用，在实际项目中我们通常会使用连接池来管理连接对象。

### 使用连接池连接

基于JDBC的连接池通常都会实现`javax.sql.DataSource`接口。
Loryn提供了一个`Database.connect(dataSource: DataSource)`方法以支持使用连接池连接数据库。
使用此方法并不会限制连接池的实现，我们可以使用任何实现了`DataSource`接口的连接池，如HikariCP、C3P0、Druid等。

```kotlin
val dataSource = SingleConnectionDataSource()
val database = Database.connect(dataSource)
```

当对数据库进行操作时，Loryn会从连接池中获取一个连接对象，并在操作完成后将连接对象归还给连接池，从而避免了频繁创建和关闭连接的开销。
对于实际项目的绝大多数情况而言，使用连接池能够显著提升系统性能与吞吐量，
因此我们建议在实际项目中使用连接池来管理连接对象，并使用此方法连接数据库。

### 连接多个数据库

对于大型项目而言，我们可能需要连接多个数据库，此时我们可以通过上述几种方式，多次调用`Database.connect()`方法来连接多个数据库。

## 事务管理
