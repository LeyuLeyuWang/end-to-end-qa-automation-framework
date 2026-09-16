# V4 开发交接

按用户决定跳过 V3，新增独立 PostgreSQL/JDBC 测试模块。移动端前置和验收项不适用，不宣称存在 Appium 覆盖。

## 实现

Compose 自动初始化 users、orders、order_items，只有虚构本地数据。默认映射 localhost:55432，固定官方 postgres:17.11-alpine 和 pgJDBC 42.7.13。

DatabaseConfig 从系统属性/环境变量读取 DB_HOST、DB_PORT、DB_NAME、DB_USER、DB_PASSWORD。DatabaseClient 直接使用 JDBC 和 PreparedStatement、try-with-resources、RowMapper，无 ORM，客户端不含断言。

BaseDatabaseTest 每个测试新建一个禁用 autoCommit 的连接，结束时 rollback 和 close。八个场景覆盖 SELECT/INSERT/UPDATE/DELETE/JOIN、金额合计、外键拒绝和参数绑定。

testng.xml 加入数据库类，默认全量执行 46 个场景；web 和 api 分组无需启动数据库，database 分组无需浏览器。Web/API 场景与断言未修改。

## 给教学聊天

提供 docker/、src/main/java/database/、src/test/java/database/、pom.xml、testng.xml 和 README。建议从 init.sql 的关系开始，再读一个 SELECT 测试、DatabaseClient 的 query/参数绑定、BaseDatabaseTest 的事务回滚，最后看 JOIN/金额/约束场景。

本地数据库并非 SauceDemo 或 Restful Booker 的后台，不应描述为跨三个系统的一体化订单验证。失败回滚探针不属于业务覆盖。

## 运行

```powershell
docker compose -f docker/docker-compose.yml up -d --wait
.\mvnw.cmd '-Dgroups=database' test
.\mvnw.cmd test
```

V2 源码和 README 已归档。下一阶段是 V5，本轮不实现 CI 或其他新技术。

## 验收（2026-09-13）

- Docker Desktop 原先未运行，启动后 Compose 拉取官方镜像，自动创建 schema/seed，容器 healthy，实际 PostgreSQL 版本 17.11。
- database-only：8 个执行全部通过，0 失败、0 错误、0 跳过。
- 最终 `clean test`：46 个执行全部通过，0 失败、0 错误、0 跳过；包括原有 24 Web、14 API 和新增 8 DB。
- DatabaseRollbackProbe 按设计故意失败。使用容器内 psql 的新连接检查，仍只有 users=1、orders=1、order_items=2，probe_rows=0；通过和失败两种路径均未保留测试数据。
- 验收后无残留 ChromeDriver。PostgreSQL 测试容器保持运行，便于用户练习；用 README 中的 down 命令可停止并保留数据卷。
- 业务报告 target/surefire-reports，故意失败探针报告 target/db-probe-reports；本地日志 .tools/v4-database.log、v4-full.log、v4-probe.log。
