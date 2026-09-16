# 12 JDBC 与 SQL：从连接到事务回滚

> 本章目标：读懂 DatabaseClient，理解连接、参数化查询、行映射和事务范围。

## 12.1 数据库模块为什么独立存在

这里的数据库是本地测试 fixture：一个可控的练习对象，有已知 schema 和 seed。它展示你能用 SQL 核对数据、用 JDBC 访问数据库、管理测试事务。没有连接 SauceDemo 或 Restful Booker 的私有数据库。

数据库测试与 UI 测试观察层级不同。直接 SQL 可以发现约束与数据关系问题，但不证明 UI 会正确调用业务逻辑。更不能根据本 fixture 的字段推测第三方应用后端结构。

## 12.2 Connection、Statement、ResultSet 的分工

Connection 表示与数据库的会话；PreparedStatement 表示参数化命令；ResultSet 表示查询结果游标。DriverManager 通过 pgJDBC 驱动建立 PostgreSQL 连接。JDBC 是 Java 标准接口，驱动实现具体数据库通信。

{{source:src/main/java/database/DatabaseConfig.java}}

URL 指向 localhost:55432/qa_fixture。用户密码放 Properties，不直接拼在 URL。setting 的优先级为系统属性 → 环境变量 → 默认值。连接失败会包装更可操作的上下文，并保留 SQLSTATE 和原异常，便于看到根因。

本地默认凭据仅用于演示容器，不是生产密码范式。Maven 的 `-DDB_PORT` 只配置 Java；Docker 端口也要用对应环境变量保持一致，单改一端连接会失败。

## 12.3 query 怎样把一张结果表变成 List<T>

{{source:src/main/java/database/DatabaseClient.java}}

query 先 prepare，再 executeQuery。ResultSet 初始位于首行之前，每次 rows.next 成功才可以读取当前行。mapper 把当前行转成 Java 值，添加到 result。循环结束关闭 ResultSet/statement，返回已脱离连接游标的普通 List。

把 ResultSet 直接返回调用者会把资源生命周期推给外部，容易读到已关闭对象或忘记关闭。RowMapper 在资源有效期间完成转换，是当前小框架的明确边界。

update 使用 executeUpdate，返回影响行数，适合 UPDATE/DELETE 和不返回结果集的写入。项目的 INSERT RETURNING id 使用 query，因为 SQL 返回结果行。方法名 query 不等于“永远只读”，实际 SQL 决定是否写入。

## 12.4 参数化为什么有用

SQL `WHERE email = ?` 把结构与参数分开；setObject(i+1, ...) 绑定数据。JDBC 参数索引从 1 开始，Java 数组从 0 开始，因此有 i+1。

教学反例（项目没有这样写）：

```java
String sql = "SELECT id FROM users WHERE email = '" + email + "'";
```

拼接会让输入有机会改变 SQL 结构。参数绑定让 `' OR '1'='1` 作为一个普通 email 值，不能改写 WHERE 的含义。参数占位符主要绑定值，不能直接把任意表名或 ORDER BY 关键字当普通参数；动态结构需要白名单等另外设计。

这条演示测试不等于全站安全测试；它只证明此查询路径把特定输入作为值处理。

## 12.5 autoCommit=false 的意义

默认自动提交模式下，一些独立语句会各自完成事务。这里显式关闭 autoCommit，使测试中的操作处在同一连接事务内。后面的 SELECT 能读取自己在该事务中刚写的数据；不需要先 commit 才能验证。

结束时 rollback 撤销该事务尚未提交的修改，随后关闭连接。因此每个测试能读到同样的 seed，又能自由做临时 INSERT/UPDATE/DELETE。

边界非常重要：回滚只影响这条连接控制的事务。若你通过 HTTP 调用某服务，而服务用它自己的连接提交了数据，你在测试 JDBC 连接上 rollback 无法撤销它。当前数据库与 API 清理方式不同正是因为控制范围不同。

## 12.6 事务不是时间机器

PostgreSQL 序列/identity 的编号消耗不会像普通行修改一样回滚。测试不应要求新 ID 一定连续。回滚后“少了一个数字”不代表残留数据。

外键违反等错误可能让当前事务进入失败状态，需要 rollback 后才能恢复。当前测试预期收到异常，然后由后置清理结束事务，不继续假装查询仍正常。

close 先尝试 rollback，再无论回滚是否成功都尝试 close；两个错误同时发生时保留 suppressed。构造时 setAutoCommit 失败也会关闭已建立连接。这些分支是资源责任，而不是为了写得复杂。

{{source:src/test/java/database/BaseDatabaseTest.java}}

@BeforeMethod 建一个 DatabaseClient；@AfterMethod 调 close 并把字段置 null。当前 db 是普通字段，因此维持串行执行前提。改为方法并行之前，必须重新设计连接所有权，不能只在 XML 加一个属性。

<details><summary>自测：测试事务中 INSERT 后 SELECT 看到了订单，这能证明另一个应用连接也已看到订单吗？</summary>

不能。当前连接能读自己的未提交数据；其他连接的可见性由提交与事务隔离规则决定。项目证明当前事务内写入和查询行为，未验证跨连接已提交持久化。

</details>

官方背景：[PostgreSQL 事务](https://www.postgresql.org/docs/17/tutorial-transactions.html)。

**English checkpoint:** “Each database test uses its own connection and rolls back its transaction. This isolation does not undo writes committed by an external service.”
