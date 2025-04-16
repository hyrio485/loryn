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
        where { it.id eq 101 }
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
极端情况下我们可能需要某些只会在特定场景使用或是只使用一次的表结构，此时我们可以使用Kotlin的匿名对象表达式来定义数据表描述对象。

灵活使用Kotlin的语法特性可以帮助我们减少重复代码、提高项目的可维护性。

## SqlType

# 最佳实践

在实际项目中，我们需要首先对我们可能用到的表进行描述，方便后续

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

当前，项目已支持主流关系型数据库的 90% 常用语法，并在三个中大型项目中验证了生产可用性。
我们始终相信，优秀的工具应当让开发者专注于业务逻辑而非框架约束。
如果您也厌倦了在 SQL 字符串和抽象过度之间反复权衡，欢迎体验 Loryn 。

——您的每一次 Star 都是对「简洁之道」的投票，每一个 Issue 都是推动项目向前的燃料。~~

https://github.com/hyrio485/loryn/issues/new
