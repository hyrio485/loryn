<h1>Loryn</h1>

[![Apache 2.0](https://img.shields.io/badge/license-Apache%202-red.svg)](LICENSE)
[![gitee star](https://gitee.com/hyrio485/loryn/badge/star.svg?theme=dark)](https://gitee.com/hyrio485/loryn/stargazers)
[![github star](https://img.shields.io/github/stars/hyrio485/loryn.svg)](https://github.com/hyrio485/loryn/stargazers)

Loryn是为Kotlin量身定制的，基于JDBC的极简ORM框架，它提供了强类型的SQL DSL，旨在减少我们手动拼接SQL出错的可能。
同时，Loryn还提供了一个简单的对象关系映射（ORM）功能，允许我们将数据库表映射到Kotlin数据类，从而简化了数据访问和操作的过程。

Loryn基于Apache 2.0协议开源，欢迎大家使用和贡献代码。
如果你在使用过程中遇到问题或有任何建议，请随时在GitHub或Gitee上提交issue，我们会尽快回复和处理。

# 开始

## 特性

- **轻量级**：Loryn是一个轻量级的ORM框架，无需配置文件、注解等，无三方依赖，易于集成和使用。
- **高性能**：Loryn基于JDBC实现，性能接近原生JDBC，未使用反射等性能开销较大的特性，适合高并发场景。
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
    where((it.id eq 101) or (it.userName like "%白%"))
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

> 目前仅支持使用无参构造器创建实体类，使用属性引用或使用getter/setter方法访问属性。

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

# 连接管理

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
需要注意的是，Loryn在方法调用时会自动加载JDBC驱动，同时使用连接获取数据库元信息，之后立刻关闭连接。
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

数据库事务是数据库管理的一个重要概念，它是指一组操作要么全部成功，要么全部失败。Loryn对JDBC事务进行了封装，提供了简单易用的事务管理API。

### 简单事务

数据库连接对象提供了`useTransaction()`函数来执行简单事务。该方法接收一个闭包函数作为参数，在闭包函数中执行数据库操作。

```kotlin
database.useTransaction {
    // 在事务中执行一组操作
}
```

在闭包函数中，我们可以执行多条SQL语句，这些语句会在同一个事务中执行。当代码块中抛出异常时，事务会自动回滚。
默认情况下，所有Throwable异常都会导致事务回滚，但我们也可以通过指定`useTransaction`方法的`rollbackFor`参数来指定哪些异常会导致事务回滚。

事务代码示例：

```kotlin
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
```

需要注意的是，`useTransaction`函数是可重入的，因此可以嵌套使用，但是内层并没有开启新的事务，而是与外层共享同一个事务。

### 事务管理器

在某些情况下，简单地使用`useTransaction()`方法来管理事务可能不够灵活，
例如需要在多个线程中共享同一个事务，或者需要在不同的数据库之间进行分布式事务操作，亦或是需要在满足特定的情况下才回滚事务。
此时，我们可以使用`database.transactionManager`方法获取事务管理器对象来管理事务。

示例代码：

```kotlin
val transactionManager = database.transactionManager
val transaction = transactionManager.newTransaction(isolation = TransactionIsolation.READ_COMMITTED)
var throwable: Throwable? = null

try {
    // do something...
} catch (e: Throwable) {
    throwable = e
    throw e
} finally {
    try {
        if (shouldRollback(throwable)) transaction.rollback() else transaction.commit()
    } finally {
        transaction.close()
    }
}
```

在默认情况下，Loryn使用`JdbcTransactionManager`来作为`TransactionManager`的实现类，这是基于JDBC提供的功能来实现的事务管理器。
而对于Spring项目而言，更推荐将事务管理的功能交由Spring来处理，
此时Loryn会使用`SpringManagedTransactionManager`来作为`TransactionManager`的实现类，详见下一章节。

## 与 Spring 集成

对于服务端项目而言，Spring是一个非常流行的框架。Spring JDBC提供了对JDBC的封装，简化了数据库操作的复杂性。
Loryn对Spring的集成基于Spring JDBC的功能，其中也包含了对Spring事务管理的支持。

当需要Loryn与Spring集成时，请保证项目直接或间接地引入了Spring JDBC的依赖：

```xml

<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-jdbc</artifactId>
  <version>${spring.version}</version>
</dependency>
```

### 创建Database对象

Loryn提供了`Database.connectWithSpringSupport()`方法，传入`DataSource`对象即可完成对Spring的集成：

```kotlin
@Configuration
class LorynConfiguration {
    @Bean
    fun database(dataSource: DataSource) = Database.connectWithSpringSupport(dataSource)
}
```

> 注意：在Loryn与Spring集成后，`database.useTransaction()`函数将不再可用；开启新事物需要借助Spring的事务管理器或相关注解实现。

### 异常转换

除了事务管理，Spring JDBC还提供了异常转换的功能，
借助此功能可以实现将JDBC驱动抛出的`SQLException`转换为Spring的`DataAccessException`异常，这样做有很多好处：

- **统一异常体系，屏蔽数据库差异，提供更清晰的异常分类与语义**：
  传统JDBC抛出`SQLException`，但不同数据库厂商的错误码（Error Code）和SQL状态码（SQLState）差异较大。
  Spring通过`DataAccessException`及其子类（如`DuplicateKeyException`、`DeadlockLoserDataAccessException`
  等）将这类底层异常统一封装，开发者无需关注具体数据库实现，只需处理Spring的异常类型
  例如，当插入重复主键时，无论底层是MySQL还是Oracle，Spring均抛出`DuplicateKeyException`，代码兼容性更强。
- **异常类型为运行时异常，减少冗余代码**：
  JDBC的`SQLException`是Checked异常，强制开发者使用try-catch处理，导致代码冗余。
  而`Spring的DataAccessException`是`RuntimeException`，开发者可根据业务需求选择性捕获异常，避免不必要的异常处理逻辑。
  不过受检异常的概念仅存在于Java程序中，Kotlin中并不存在此问题。

除此之外，Spring JDBC的异常转换还有其他一些好处，如提供更丰富的异常信息、支持自定义异常转换等，此处不再展开。
Loryn在与Spring集成后，将使用`SQLErrorCodeSQLExceptionTranslator`进行异常转换，用户无需额外配置。

示例程序：

```kotlin
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
```

上述代码首先尝试添加一条用户记录，如果主键冲突，则捕获`DuplicateKeyException`异常并更新该记录。

## 日志输出

在使用Loryn时，程序会将其内部一些操作的参数以日志的形式进行输出，如生成的SQL语句、参数值、影响行数等。
目前Loryn仅支持使用SLF4J作为日志框架，用户可以根据需要选择合适的日志实现，如Logback、Log4j2等。

在成功连接到数据库后，Loryn会以`INFO`级别输出一条日志，内容为连接的数据库URL、用户名、驱动类名等信息。
在执行SQL语句时，Loryn会以`DEBUG`级别输出日志，内容为生成的SQL语句、参数值、影响行数（DML）等信息。

在实际的工程化项目中，我们可能不希望所有的表结构都使用同样的loggerName进行日志输出，这样不方便我们在仅关注某些特定表的日志时进行过滤。
这时我们可以使用`database.withLogger()`方法动态变更loggerName。新生成的`database`对象不会对之前的对象产生影响，
且在使用新对象进行数据库操作时会使用新的loggerName进行日志输出。详细内容可参考[最佳实践](#最佳实践)章节。

# SQL DSL 及实体映射

Loryn提供了丰富的SQL DSL语法来支持各种DQL与DML操作，用户可以根据需要选择合适的DSL语法来构建SQL语句。

为了方便说明，以下示例均采用[快速开始](#快速开始)一节中的表结构及数据。

## 数据表描述对象

在使用Loryn操作数据库之前，我们需要先定义数据表描述对象以告知Loryn数据表的结构。

根据是否绑定了实体对象，Loryn提供了两种数据表描述对象（示例代码可分别参考[快速开始](#快速开始)
与[实体类与列绑定](#实体类与列绑定)章节）。

- `Table`：用于描述未绑定实体类的数据表的结构，支持指定表名、主键、列名、列类型、是否允许为null等属性，另外传入category或schema参数可实现跨库查询。
- `BindableTable`：是`Table`的子类，相较于`Table`，`BindableTable`需要额外指定实体类的无参构造方法以创建实体对象。

在类定义内部，我们使用`column()`方法来定义数据表的列。
对于`Table`类而言，我们需要传入列名及列类型（详见[SqlType](#SqlType)一节），额外可指定主键、是否允许为null等属性；
`BindableTable`类则需要额外传入绑定的实体对象的属性引用或getter/setter方法（本质上属性引用在构造方法内部也会被转换为getter/setter方法）。

`Table`与`BindableTable`均为抽象类，通常我们推荐使用Kotlin的`object`关键字来定义单例的数据表描述对象，但这不是必须的。

某些情况下我们可能需要两张除了表名外完全相同的表结构（如数据备份等场景），此时我们可以使用`class`关键字来定义数据表描述对象，
并将可变内容（如表名、数据库名等）作为参数传递。

```kotlin
sealed class Users(tableName: String) : Table(tableName) {
    val id = column("id", IntSqlType, primaryKey = true)
    val userName = column("username", StringSqlType, notNull = true)
    val createdAt = column("created_at", LocalDateTimeSqlType, notNull = true)
    val lastLogin = column("last_login", LocalDateTimeSqlType)
}

object NormalUsers : Users("tb_normal_users")

object ArchivedUsers : Users("tb_archived_users")
```

极端情况下我们可能需要某些只会在特定场景使用或是只使用一次的表结构，此时我们可以使用Kotlin的匿名对象表达式来定义数据表描述对象。

```kotlin
val tempUsers = object : Table("temp_users") {
    val id = column("id", IntSqlType, primaryKey = true)
    val userName = column("username", StringSqlType, notNull = true)
    val createdAt = column("created_at", LocalDateTimeSqlType, notNull = true)
    val lastLogin = column("last_login", LocalDateTimeSqlType)
}
```

灵活使用Kotlin的语法特性可以帮助我们减少重复代码、提高项目的可维护性。

### 绑定实体后的便捷操作

如上文所述，`BindableTable`类相较于`Table`类额外保存了绑定的实体的泛型类型与无参构造方法。
除此之外，`BindableTable`类中声明列的`column()`方法还需要指定实体列的属性引用或getter/setter方法。

当完成以上操作后，我们可以使用`Database.selectBindable()`方法来查询数据并将结果映射到实体类中。

对于DML语句，Loryn也提供了`insert()`、`update()`、`delete()`等方法通过实体类直接操作数据表而无需手动指定需要增删改的列。
对于实体的增加和修改操作，为了实现实体对象与数据库行的操作映射，
我们需要重写`BindableTable`类中的`insertColumns`、`updateColumns`属性以指定需要增删改的列。

除此之外，Loryn还支持逻辑删除（`deleteLogically()`）与乐观锁更新（`updateOptimistic()`），
这同样需要我们重写`BindableTable`类中的`revColumn`、`deletedColumn`属性以指定版本号列与逻辑删除列。

详细说明与示例代码可参考[DQL语句](#DQL语句)与[DML语句](#DML语句)章节。

## SqlType

SqlType是Loryn中对底层JDBC类型与Kotlin类型的封装，其本身是一个抽象类。
子类继承SqlType类需要实现从JDBC ResultSet中获取和设置数据的方法。

示例代码：（`IntSqlType`）

```kotlin
object IntSqlType : SqlType<Int>(JDBCType.INTEGER, Int::class.java) {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Int) {
        ps.setInt(index, parameter)
    }

    override fun doGetResult(rs: ResultSet, index: Int): Int? {
        return rs.getInt(index)
    }
}
```

Loryn预设了多种SqlType类型以支持不同的JDBC类型与Kotlin类型的映射，具体如下：

| 类型名                              | Kotlin/Java类型                             | JDBC类型（`java.sql.JDBCType`）       |
|----------------------------------|-------------------------------------------|-----------------------------------|
| `BooleanSqlType`                 | `Boolean`                                 | `BOOLEAN`                         |
| `IntSqlType`                     | `Int`/`Integer`                           | `INTEGER`                         |
| `ShortSqlType`                   | `Short`                                   | `SMALLINT`                        |
| `LongSqlType`                    | `Long`                                    | `BIGINT`                          |
| `FloatSqlType`                   | `Float`                                   | `FLOAT`                           |
| `DoubleSqlType`                  | `Double`                                  | `DOUBLE`                          |
| `DecimalSqlType`                 | `java.math.BigDecimal`                    | `DECIMAL`                         |
| `VarcharSqlType`/`StringSqlType` | `String`                                  | `VARCHAR`                         |
| `TextSqlType`                    | `String`                                  | `LONGVARCHAR`                     |
| `BlobSqlType`                    | `ByteArray`/`byte[]`                      | `BLOB`                            |
| `BytesSqlType`                   | `ByteArray`/`byte[]`                      | `BINARY`                          |
| `TimestampSqlType`               | `java.sql.Timestamp`                      | `TIMESTAMP`                       |
| `JdbcDateSqlType`                | `java.sql.Date`                           | `DATE`                            |
| `JavaDateSqlType`                | `java.util.Date`                          | `DATE`                            |
| `TimeSqlType`                    | `java.sql.Time`                           | `TIME`                            |
| `InstantSqlType`                 | `java.time.Instant`                       | `TIMESTAMP`                       |
| `LocalDateTimeSqlType`           | `java.time.LocalDateTime`                 | `TIMESTAMP`                       |
| `LocalDateSqlType`               | `java.time.LocalDate`                     | `DATE`                            |
| `LocalTimeSqlType`               | `java.time.LocalTime`                     | `TIME`                            |
| `MonthDaySqlType`                | `java.time.MonthDay`                      | `VARCHAR`                         |
| `YearMonthSqlType`               | `java.time.YearMonth`                     | `VARCHAR`                         |
| `YearSqlType`                    | `java.time.Year`                          | `INTEGER`                         |
| `UuidSqlType`                    | `java.util.UUID`                          | `OTHER`                           |
| `EnumSqlType<C>`                 | `C extends Enum<C>`                       | `OTHER`（PostgreSQL）/`VARCHAR`（其他） |
| `JsonSqlType`                    | `com.fasterxml.jackson.databind.JsonNode` | `VARCHAR`                         |

如果上述类型不能满足需求，用户也可以自定义SqlType类型，只需继承`SqlType`类并实现`doSetParameter()`与`doGetResult()`方法即可。

除此之外，`SqlType`类还提供了`transform()`方法来实现基于已有的类型导出一个新类型，
如预设的`JsonObjectSqlType`就是基于`JsonSqlType`导出的一个新类型。

```kotlin
val JsonObjectSqlType = JsonSqlType.transform(ObjectNode::class.java, { it as ObjectNode }, { it })
```

但需要注意的是，SqlType的转换在每次读取数据时都会执行一次，因此需要避免在转换方法中执行耗时的操作，以免对于实体映射的性能造成影响。

## SQL表达式

Loryn使用`SqlExpression<T>`类对SQL表达式进行了抽象，多个SQL表达式的排列组合组成了语句。

Loryn预设了多个`SqlExpression`的子类用以描述不同类型的SQL表达式，如：

- `ColumnExpression`：表示列表达式，通常用于描述数据表的列。
- `UnaryExpression`：表示一元运算符表达式，如`+`、`-`等。
- `BinaryExpression`：表示二元运算符表达式，如`+`、`-`、`*`、`/`等。
- `FunctionExpression`：表示函数表达式，如`SUM()`、`COUNT()`等。
- ……

除此之外，Loryn还预设了多个运算符（扩展函数）以快速生成不同类型的SQL表达式，详见[Operators.kt](loryn-core/src/main/java/top/loryn/expression/Operators.kt)。

### 运算符优先级

运算符从Kotlin语法层面看，我们可将其分为三类：

1. 使用`operator`关键字定义的运算符，如`+`、`-`、`*`、`/`等；
2. 使用`infix`关键字定义的运算符，如`eq`、`ne`、`lt`、`gt`等；
3. 普通的函数调用，如`in`、`like`、`isNull`等。

运算符函数可连续调用，但这必定会出现运算符优先级的相关问题。Kotlin在语法层面对其进行了处理，
具体请参考[Kotlin 语言规范](#https://kotlinlang.org/docs/reference/grammar.html#expressions)中的相关规定。

## DQL语句

在[快速开始](#快速开始)一节中，我们曾执行了一个简单的查询操作：

```kotlin
val users = database.select(Users).list { resultSet ->
    resultSet[Users.id] to resultSet[Users.userName]
}
```

在这个查询中，我们使用了`select()`方法来创建一个`SelectStatement`对象，并使用`list()`方法来执行查询并将结果集映射为实体对象，最后返回一个列表。

对于绑定了实体类的数据表描述对象，我们可以使用`selectBindable()`方法来创建一个`BindableSelectStatement`对象。
在调用`list()`方法时，我们快速地将结果集映射为实体对象。

> 同样的，Loryn也提供了`one()`方法来获取单条记录，当查询结果包含多条记录时会抛出异常。

如需要对表数据进行过滤，可在Builder方法中设置过滤条件；同样的，也可设置排序条件、分组条件、分页参数等：

```kotlin
database.select(Users) {
    where(it.id gte 101)
    orderBy(it.userName.toOrderBy())
}
```

### 设置别名

若希望在后续的条件中使用之前列的值，则可以使用`alias()`方法为列设置别名：

```kotlin
val i = Users.id.aliased("i")
database.select(Users) {
    column(i)
    where(i gte 102)
}.list { it[i] }.also(::println)
```

生成的SQL为：

```sql
SELECT id AS i
FROM users
WHERE id >= ?;
```

### 联表查询

Loryn支持联表查询，用户可以对`QuerySource`对象使用`join()`方法来连接其他表：

```kotlin
val u = Users.aliased("u")
val o = Orders.aliased("o")
var userNameColumn = u[Users.userName]
var productIdColumn = o[Orders.productId]
database.select(u.join(o, joinType = JoinQuerySource.JoinType.LEFT, on = u[Users.id] eq o[Orders.userId])) {
    columns(userNameColumn, productIdColumn)
}.list { it[userNameColumn] to it[productIdColumn] }.also(::println)
```

生成的SQL为：

```sql
SELECT u.username, o.product_id
FROM users AS u
         LEFT JOIN orders AS o ON u.id = o.user_id;
```

在上述代码中，我们使用了`join()`方法来连接`Users`与`Orders`表，并指定了连接类型为左连接（`LEFT JOIN`）。

> 提示：对于连接的两张表不存在同名字段时，由于不会造成歧义，因此此种情况下无需使用`aliased()`方法为表设置别名。

对于左、右、内连接，Loryn提供了`leftJoin()`、`rightJoin()`、`innerJoin()`等方法来简化连接操作。

### 子查询

实际业务中，我们可能需要在查询中使用子查询来实现更复杂的查询逻辑。
Loryn为`QuerySource`对象提供了扩展函数`select()`创建一个`SelectExpression`对象，是`QuerySource`的子类，可以在查询中使用。

```kotlin
database.selectBindable(BindableOrders) {
    where(it.userId `in` Users.select {
        column(it.id)
        where(it.id gte 102)
    })
}.list().also(::println)
```

生成的SQL为：

```sql
SELECT *
FROM orders
WHERE user_id IN (SELECT id FROM users WHERE id >= ?);
```

## DML语句

与DQL语句类似，Loryn也提供了丰富的DSL语法来支持各种DML操作，如插入、更新、删除等，三类操作均可按是否绑定了实体分为两类。

### 插入操作

`database.insert()`方法用于可用于插入数据，方法返回一个`InsertStatement`对象，调用其`execute()`方法即可执行插入操作。
`execute()`方法返回一个`Int`类型的值，表示插入操作影响的行数。

```kotlin
database.insert(Users) {
    assign(it.id, 101)
    assign(it.userName, "abc")
    assign(it.createdAt, LocalDateTime.now())
}.execute()
```

在上述代码中，我们使用了`insert()`方法来创建一个`InsertStatement`对象，并使用`assign()`方法来指定要插入的列及对应的值。

`insert()`方法还可指定是否使用自生成的主键；如果需要在插入数据后获取自生成的主键值，
则可以在`execute()`方法中传入一个Lambda函数，操作`ResultSet`对象来获取自生成的主键值。

```kotlin
database.insert(Users, useGeneratedKeys = true) {
    assign(it.id, 101)
    assign(it.userName, "abc")
    assign(it.createdAt, LocalDateTime.now())
}.execute {
    if (it.next()) {
        println("Inserted user with id: ${it.getInt(1)}")
    } else {
        throw IllegalStateException("No generated keys returned")
    }
}
```

除了`INSERT ... VALUES`语法，Loryn还支持`INSERT ... SELECT`语法：

```kotlin
database.insert(Users) {
    columns(it.userName, it.createdAt)
    select(Users.select {
        columns(it.userName, it.createdAt)
        where(it.id eq 101)
    })
}.execute()
```

对于绑定了实体的表，我们可直接插入实体对象：

```kotlin
val user = UserPo().apply { /* 省略字段赋值操作 */ }
database.insert(BindableUsers, user, useGeneratedKeys = true)
```

注意这里的`insert()`执行后将会直接将实体对象的所有字段插入到数据库中，无需额外调用`execute()`方法；
直接插入实体对象需要数据表描述类重写`insertColumns`属性以指定需要插入的列。

### 删除与更新操作

Loryn的删除与更新操作与插入操作类似，也可按是否绑定了实体分为两类。

```kotlin
val effects1 = database.delete(Users) { where(it.id eq 101) }
val effects2 = database.delete(BindableUsers, user)
val effects3 = database.update(Users) {
    set(it.userName, "abc")
    where(it.id eq 101)
}
val effects4 = database.update(BindableUsers, user)
```

需要注意的是，`delete()`方法与`update()`方法均会直接返回影响的行数，因此无需额外调用`execute()`方法。
同样，使用实体直接进行修改操作的前提是数据表描述类重写`updateColumns`属性以指定需要更新的列。

初次之外，Loryn还支持逻辑删除与乐观锁更新操作：

```kotlin
val effects5 = database.deleteLogically(BindableUsers, user)
val effects6 = database.updateOptimistic(BindableUsers, user)
```

> 注意：逻辑删除与乐观锁更新仅支持实体对象操作，前者依赖删除标记字段，后者依赖版本号字段，
> 分别需要在数据表描述类中重写`deletedColumn`与`revColumn`属性以指定删除标记字段与版本号字段才能完成对应操作。

对于某些数据库如MySQL而言，在SQL层面允许同时插入多条数据，
或是提供了如`ON DUPLICATE KEY UPDATE`的语法来实现插入或更新操作，详见[使用方言包](#使用方言包)一节。

## 数据库方言

数据库方言指不同数据库在SQL标准之外实现的差异化特性。
虽然SQL语言存在统一规范，但实际应用中，各数据库（如MySQL、Oracle）会基于性能优化或场景需求，形成各自的扩展语法（如分页参数、内置函数等）。

Loryn框架的核心模块（loryn-core）主要实现标准SQL的通用支持，如需使用特定数据库的独有功能，则需借助方言模块。
方言包通过适配不同数据库的语法规则、数据类型映射及特有函数，让开发者能够轻松调用各类数据库所读有的特性，同时保持ORM层代码的统一。
Loryn为主流的数据库方言提供了支持包，只需要引入对应的依赖并在创建连接时指定要使用的方言即可。各方言包基于Java的SPI机制自动加载。

由于作者精力有限，很难较为全面的覆盖数据库的所有方言特性；不过Loryn是一个具备良好扩展性的框架，用户可以根据自己的需求实现自定义的方言包或对已有方言包进行补充。
欢迎社区开发者为Loryn开发扩展，fork我们的仓库，提交pr，期待各位开发者的贡献！

### 使用方言包

我们以MySQL为例，我们需要在项目中引入MySQL的方言包依赖：

```xml

<dependency>
  <groupId>top.loryn</groupId>
  <artifactId>loryn-dialect-mysql</artifactId>
  <version>${loryn.version}</version>
</dependency>
```

在创建连接时，我们指定要使用的方言：

```kotlin
val database = Database.connect(
    // ... 省略其他参数
    dialect = MysqlSqlDialect()
)
```

在后续的查询中，Loryn会自动使用MySQL的方言来生成SQL语句。另外，开发者还可以使用方言包中定义的数据库特有函数，
但需要注意的是使用此类函数生成SQL时不会校验当前数据库是否支持，对于多种数据源时需要谨慎使用。

以MySQL的`ON DUPLICATE KEY UPDATE`语法举例：

```kotlin
database.insertOrUpdate(Users) {
    assign(it.id, 101)
    assign(it.userName, "abc")
    assign(it.createdAt, LocalDateTime.now())
    set(it.userName, "abc")
}.execute()
```

或使用绑定实体的方式：（需要在定义表结构时重写`insertColumns`与`updateColumns`属性）

```kotlin
database.insertOrUpdate(BindableUsers, UserPo().apply {
    id = 101
    userName = "abc"
    createdAt = LocalDateTime.now()
})
```

生成的SQL为：

```sql
INSERT INTO users (id, username, created_at)
VALUES (?, ?, ?)
ON DUPLICATE KEY UPDATE username = ?;
```

### 分页参数

标准SQL并未**明确定义分页查询的通用语法**。主流数据库厂商的具体实现方式如下：

- **MySQL**：使用`LIMIT offset, row_count`或`LIMIT row_count OFFSET offset`
- **Oracle**：通过`ROWNUM`伪列结合子查询实现（例如
  `SELECT * FROM (SELECT t.*, ROWNUM rn FROM table t WHERE ROWNUM <=30) WHERE rn >20`）
- **PostgreSQL**：采用`LIMIT num OFFSET offset`标准写法
- **SQL Server 2012+**：引入`OFFSET...FETCH`子句（例如
  `SELECT * FROM table ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY`）

由于分页查询是在实际项目中最常用的操作之一，因此Loryn核心模块对DQL语句提供了分页查询的定义，但并未实现具体的分页查询方法，这部分内容交由方言包来实现。

在`SelectExpression.AbstractBuilder`类中，Loryn提供三个方法来设置分页参数：

- `limit()`：用于简单设置获取的行数；
- `pagination(paginationParams: PaginationParams）`：用于设置分页参数（当前页及页面大小）；
- `pagination(currentPage: Int, pageSize: Int)`：方法二重载的简易版。

代码示例：

```kotlin
val users = database.selectBindable(BindableUsers) {
    pagination(1, 10)
}
```

### 使用原生SQL

在某些场景中，开发者可能会遇到如下情况：

1. 需要处理复杂业务逻辑或数据库专有特性
2. 执行表结构变更（DDL）语句
3. 框架当前版本尚未支持的特定语法

在这些情况下，Loryn允许开发者使用原生SQL语句来操作数据库。我们建议开发者优先使用框架提供的强类型 DSL，但在必要时可通过以下方式直接操作数据库：

**方案一**：使用Loryn的列映射功能

```kotlin
val effects = database.dml(
    "INSERT INTO users (id, username, created_at) VALUES (?, ?, ?)",
    101.toSqlParam(),
    "abc".toSqlParam(),
    LocalDateTime.now().toSqlParam(LocalDateTimeSqlType),
).execute()
```

```kotlin
val idColumn = ColumnExpression("id", IntSqlType)
val userNameColumn = ColumnExpression("username", StringSqlType)
database
    .dql(
        "SELECT * FROM users WHERE id = ?",
        101.toSqlParam(),
        columns = listOf(ColumnExpression("id", IntSqlType))
    )
    .list {
        it[idColumn] to it[userNameColumn]
        // it[Users.id] to it[Users.userName]
    }
    .forEach { println(it) }
```

```kotlin
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
```

优势：自动完成结果集到实体类的映射，支持参数预编译防注入。

**方案二**：使用 JDBC 原生 API

```kotlin
val database = Database.connect("jdbc:mysql://localhost:3306/loryn_test", "root", "root")
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
```

适用场景：存储过程调用、数据库专有函数等深度定制化操作。

> 需要注意的是：使用原生 SQL 将导致框架的强类型校验、跨库兼容等核心特性失效，可能引发 SQL 注入风险、数据库方言耦合、执行计划不可控等问题，
> 同时会破坏代码可维护性与框架的统一监控体系。建议仅作为临时方案并严格限制在隔离层，使用时采用预编译参数并标注废弃计划。

# 设计理念

## SQL是如何生成的？

我们以一个简单的联表查询SQL为例：

```sql
SELECT u.username, o.product_id
FROM users AS u
         LEFT JOIN orders AS o ON u.id = o.user_id
WHERE u.id >= 102;
```

在Loryn中我们可以使用如下代码来生成上述SQL：

```kotlin
val u = Users.aliased("u")
val o = Orders.aliased("o")
var userIdColumn = u[Users.id]
var userNameColumn = u[Users.userName]
var productIdColumn = o[Orders.productId]
database.select(u.join(o, joinType = JoinQuerySource.JoinType.LEFT, on = userIdColumn eq o[Orders.userId])) {
    columns(userNameColumn, productIdColumn)
    where(userIdColumn gte 102)
}.list { it[userNameColumn] to it[productIdColumn] }.also(::println)
```

在程序内部会构建如下所示的AST（Abstract Syntax Tree，抽象语法树）：

```
SelectExpression
├── (columns): *(empty)
├── (from): JoinQuerySource
│   ├── (left): Table
│   │   ├── (name): Users
│   │   └── (alias): u
│   ├── (right): Table
│   │   ├── (name): Orders
│   │   └── (alias): o
│   ├── (joinType): LEFT
│   └── (on): InfixExpression
│       ├── (operator): eq(=)
│       ├── (expr1): Column
│       │   ├── (table): Users
│       │   │   └── (alias): u
│       │   └── (name): id
│       └── (expr2): Column
│           ├── (table): Orders
│           │   └── (alias): o
│           └── (name): user_id
└── (where): InfixExpression
    ├── (operator): gte(>=)
    ├── (expr1): Column
    │   ├── (table): Users
    │   │   └── (alias): u
    │   └── (name): id
    └── (expr2): SqlParam
        ├── (sqlType): IntSqlType
        └── (value): 102
```

AST的每个节点都是`SqlAppender`接口的一个子类，表示SQL语句中的一个表达式。
在构建SQL语句时，Loryn会创建一个`SqlBuilder`对象用以构建SQL语句。
扩展的方言包需要提供一个创建`SqlBuilder`子类对象的方法，核心模块将在需要创建SQL语句时调用该方法以创建`SqlBuilder`对象。

`SqlBuilder`会按照AST的结构访问每个节点，节点内部需要实现`buildSql()`方法向`SqlBuilder`对象添加SQL语句片段。
当所有节点都访问完成后，`SqlBuilder`对象会将SQL语句拼接完成，同时返回所有的参数值，最后调用JDBC的API来执行SQL语句。

生成SQL语句的过程用到了访问者模式（Visitor Pattern）。当需要适配新的数据库时，只需要实现一个新的`SqlBuilder`类，
重写对应方法，根据不同的节点类型决定如何拼接SQL语句即可。

## 如何自定义运算符及函数

Loryn允许用户自定义SQL运算符及函数，只需根据需要定义返回值类型为`SqlExpression<T>`的函数即可。

定义运算符示例：

```kotlin
infix fun <C> SqlExpression<C>.eq(other: SqlExpression<C>) = InfixExpression("=", this, other, BooleanSqlType)
```

对于函数，Loryn预设了[InfixExpression.kt](loryn-core/src/main/java/top/loryn/expression/InfixExpression.kt)及
[FunctionExpression.kt](loryn-core/src/main/java/top/loryn/expression/FunctionExpression.kt)用于描述函数的表达式。
用户可以根据需要定义函数表达式的名称、参数列表、返回值类型等信息。

```kotlin
fun <T> SqlExpression<T>.ifNull(default: SqlExpression<T>) = FunctionExpression("IFNULL", sqlType, this, default)
fun <T> Column<T>.max() = FunctionExpression("MAX", sqlType, this)
```

Loryn是一个高度可扩展的框架，用户可以根据需要自定义运算符及函数，以满足不同的业务需求。

# 最佳实践

Loryn作为一个轻量级的ORM框架，旨在为开发者提供一种简单易用的方式来操作数据库，而在实际项目中，光有数据库连接对象和表结构描述对象是不够的，
我们还需要考虑如何管理这些对象的生命周期、如何处理异常、如何进行日志输出等问题。以下是最佳实践的一些建议：

1. 根据需要对每个可能操作的数据库创建`Database`对象，并保存于全局变量或容器中（如Spring容器）。
2. 对所有可能用到的表创建描述对象，推荐使用`object`关键字来定义单例的数据表描述对象并绑定实体类。
   > 实体及数据表描述对象命名规范：数据库表绑定的实体对象应为名词的单数形式，且以`Po`结尾，如`UserPo`、`ProductPo`等；
   数据库表描述对象应为名词的复数形式，如`Users`、`Products`等。
3. 根据业务划分功能边界，创建不同的DAO（Data Access Object）类来处理不同模块的数据库操作。
   这里的业务边界可以是一个功能模块、一个业务场景、一个数据表等，具体划分方式可以根据项目的实际情况来决定。
   > DAO类的命名规范：`功能模块名 + Dao`，如`UserDao`（处理用户相关的数据库操作）、`ProductDao`（处理商品相关的数据库操作）等。
4. 在DAO类中注入需要的数据库连接对象，同时借助`withLogger()`方法创建一个新的`Database`对象以实现不同的日志输出。
5. 数据库连接对象只在DAO类内部使用，不暴露给外部，避免外部直接操作数据库连接对象；在DAO类内部只操作指定功能边界内的数据表，避免跨模块操作数据库。

# 关于 Loryn 项目

## 项目缘起

2025 年春季，笔者在主导公司内部使用的新系统改造时，深刻体会到传统 ORM 框架在复杂业务场景中的掣肘。
项目需要同时对接 MySQL 与信创数据库，且包含大量的单表与联表查询、修改操作，而现有工具要么依赖手写的SQL，
较难支持多类型的数据库（如 MyBatis），要么在多方言支持上存在明显短板（如 JPA 部分实现）。
更棘手的是，业务中存在大量动态条件拼接需求——例如根据用户输入实时生成包含数十个筛选条件的统计查询，
这迫使团队在「手写原生 SQL 导致代码臃肿」和「强行抽象造成逻辑晦涩」之间反复妥协。

彼时，市面上虽不乏声称支持「多数据库」「动态查询」的框架，但实际体验往往差强人意：
某些工具通过字符串拼接生成 SQL 存在注入风险；另一些则因过度封装导致调试困难。
在尝试改造多个开源方案未果后，笔者偶然接触到 Kotlin 生态的 Ktorm 框架。
其通过 DSL 将 SQL 表达能力与类型安全结合的设计令人耳目一新——无需手写 SQL 字符串，亦不必依赖反射或注解魔法，
仅凭代码即可构建出类型可推导的查询逻辑。这种优雅的实现方式成为 Loryn 诞生的直接启发。

## 定位与初心

Loryn 并非要替代成熟的 ORM 生态，而是聚焦于解决两个核心痛点：

- **无侵入的多数据库适配**：开发者应能通过同一套 API 操作不同数据库，且无需为兼容性妥协功能完整性；
- **复杂查询的类型安全表达**：在避免手写 SQL 的前提下，构建出可读性强、编译期可校验的动态查询逻辑。

为实现这一目标，项目初期即确立了三项原则：

- **轻量透明**：不强制要求实体类继承特定接口或添加注解，降低框架对业务代码的侵入性；
- **渐进式抽象**：基础操作开箱即用，复杂场景允许逐步引入更高级的 DSL 组件；
- **可预测性**：所有生成的 SQL 需具备明确的行为逻辑，杜绝「魔法式」的隐式转换。

## 开发历程中的反思

首版原型仅用两周便实现了基础查询功能，但在实际测试中暴露出设计缺陷。
例如，早期版本要求将数据库列定义直接嵌入实体类——这种强耦合设计虽简化了初期开发，
但这导致同一实体无法适配不同表结构（常见于分库分表或多租户场景）。
此外，为追求「纯类型安全」而引入的深层泛型嵌套，反而让 IDE 提示变得混乱，甚至在某些情况下连方法补全都难以识别。

这些问题促使笔者对核心 API 进行了重构。经过两个月的调整，列定义与实体完成解绑，开发者可通过注册表结构实现动态切换；
泛型层级也被大幅简化，使得代码提示清晰度提升 60% 以上。
~~尽管重构过程充满挑战（例如需要重新设计 QuerySource 相关逻辑与实体对象绑定相关机制），
但最终还是达到了较为满意的效果，呈现的 API 显著降低了用户的学习成本。

## 致谢与期待

Loryn 的成长始终站在巨人的肩膀之上。特别感谢 Ktorm 项目在类型安全 DSL 领域的开创性实践，其源码结构为 Loryn 的模块划分提供了重要参考；
同时也受益于社区关于 Exposed、Jimmer 等框架的讨论，这些声音帮助我们更精准地定位了差异化价值。

目前，项目已支持主流关系型数据库的 90% 常用语法，并在三个中大型项目中验证了生产可用性。
我们始终相信，优秀的工具应当让开发者专注于业务逻辑而非框架约束。
如果您也厌倦了在 SQL 字符串和抽象过度之间反复权衡，欢迎体验 Loryn 。

您的每一次 Star 都是对「简洁之道」的投票，每一个 Issue 都是推动项目向前的燃料。~~

[点我快速提交GitHub Issue](#https://github.com/hyrio485/loryn/issues/new)

[点我快速提交Gitee Issue](#https://gitee.com/hyrio485/loryn/issues/new)
