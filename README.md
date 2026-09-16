# QA Automation Framework — V5

V5 新增 Chrome/Firefox、无头模式、Web 类并行、Allure 和 GitHub Actions 配置。云端运行尚待上传仓库后验收；本地结果见 [V5 交接](docs/V5_HANDOFF.md)。

Java QA 自动化学习项目。**24 个 Web + 14 个 API + 8 个数据库场景，共 46 个执行、35 个业务 @Test 方法。** V3 Appium 按用户决定暂缓，当前路线为 V0 → V1 → V2 → V4 → V5。

Selenium 测试 SauceDemo；REST Assured 测试 Restful Booker；JDBC 测试独立的本地 PostgreSQL。这三个系统互不连接。

**The PostgreSQL module is a local test fixture used to demonstrate database validation with JDBC and SQL. It is not connected to SauceDemo's private backend.** 它也不是 Restful Booker 的数据库。

## 环境和运行

需要 JDK 17+、JAVA_HOME、Chrome 或 Firefox、Docker Desktop Linux 引擎。数据库分组不需要浏览器，Web/API 分组不需要 Docker。首次运行需要联网下载依赖、驱动、镜像。

在项目根目录执行：

```powershell
docker compose -f docker/docker-compose.yml up -d --wait
.\mvnw.cmd test
.\mvnw.cmd '-Dgroups=database' test
.\mvnw.cmd '-Dgroups=api' test
.\mvnw.cmd '-Dgroups=web' test
.\mvnw.cmd '-Dgroups=smoke' test
.\mvnw.cmd '-Dgroups=negative' test
```

已安装 Maven 可用 `mvn test -Dgroups=database`。macOS/Linux 入口为 `sh ./mvnw test`，当前实测平台是 Windows。版本固定在 pom.xml 和 Compose：Maven 3.9.11、PostgreSQL 17.11-alpine、pgJDBC 42.7.13，Web/API 依赖版本保留。

| 分组 | 展开执行数 |
|---|---:|
| web | 24 |
| api | 14 |
| database | 8 |
| regression（全部） | 46 |
| smoke（跨三个模块） | 9 |
| negative（跨三个模块） | 18 |

分组是重叠标签，不要相加作为测试总数。默认 `test` 包含数据库，必须先启动 Compose。只选 web 或 api 可以不启动数据库。

## 数据库模块（V4 引入）

```text
docker/docker-compose.yml                    镜像、端口、健康检查、持久卷
docker/init/init.sql                         建表和种子数据
.env.example                                 本地配置示例
src/main/java/database/DatabaseConfig.java    读取配置、建立 JDBC 连接
src/main/java/database/DatabaseClient.java    参数化 SQL、映射结果、事务回滚
src/test/java/database/BaseDatabaseTest.java  每次测试连接生命周期
src/test/java/database/DatabaseValidationTest.java 8 个数据库业务测试
src/test/java/database/DatabaseRollbackProbe.java  显式失败诊断
```

没有 ORM、数据库连接池或自建后端。原有 Web Page Object、API 客户端、失败诊断保持独立。

## 表结构与数据

- users：id、唯一 email、active/inactive 状态。
- orders：id、user_id 外键、pending/paid/cancelled 状态、非负金额、创建时间。
- order_items：id、order_id 外键、商品名称、正数数量、非负单价；删除订单级联删除明细。

初始化 1 个用户 seed@example.test、1 个 paid 订单、2 个明细，金额 29.99 + 9.99 = 39.98。数据均为虚构本地样例。

SQL 在空数据卷首次启动时自动执行。已有数据卷重启不会重新运行初始化，修改 init.sql 也不会自动迁移旧数据库。[官方 PostgreSQL 镜像说明](https://hub.docker.com/_/postgres)

## 八个数据库场景

| 方法 | 验证 |
|---|---|
| seededUserIsActive | 查询种子用户并验证状态 |
| insertedOrderCanBeRead | INSERT RETURNING 后 SELECT 核对金额和状态 |
| orderStatusCanBeUpdated | UPDATE 影响一行，查询确认状态 |
| orderCanBeDeleted | DELETE 影响一行，查询确认不存在 |
| joinAssociatesOrderWithItsUser | JOIN 验证订单对应用户 |
| seededOrderTotalMatchesLineItems | JOIN、SUM、COUNT 验证明细数量和金额合计 |
| nonexistentUserCannotOwnOrder | 无效 user_id 被外键约束拒绝，SQLSTATE 23503 |
| parameterizedInputCannotChangeQueryMeaning | 注入式文本作为普通参数处理，不影响种子数据 |

最后一个场景是参数绑定演示，不宣称全面安全测试。金额使用 BigDecimal，序列 ID 不要求连续。

## JDBC 和测试隔离

每次测试建立独立连接、关闭 autoCommit。所有 SQL 使用 PreparedStatement，输入通过 setObject 绑定；ResultSet 和 Statement 使用 try-with-resources 关闭。RowMapper 将结果转换为类型明确的值或 record 后返回，客户端无测试断言。

`@AfterMethod(alwaysRun = true)` 无论测试成功还是断言失败，都调用 rollback 并关闭连接。外键错误造成事务中止后也由 rollback 清理。序列值不会随事务回滚，这是 PostgreSQL 正常行为。没有自动重试或静默跳过数据库连接问题。

## 配置

Java 配置优先级：同名 `-DDB_*` 系统属性 → shell 环境变量 → 默认值。

| 名称 | 默认值 |
|---|---|
| DB_HOST | localhost |
| DB_PORT | 55432 |
| DB_NAME | qa_fixture |
| DB_USER | qa_local |
| DB_PASSWORD | qa_local_only |

Compose 项目名 qa-automation-v4，端口只绑定本机 127.0.0.1:55432，避免占用常用的 5432。凭据仅适用于本地测试。

例如在 PowerShell 执行 `$env:DB_PORT = '55433'`，然后运行 Compose 和 Maven，可让两者读取相同端口。Maven 的 `-DDB_PORT` 只影响 Java，不影响容器映射。

`.env.example` 是示例。只复制为 `.env` 不会让 Java 自动读取它，建议使用 shell 环境变量同时配置两端。真实密码不要写入源码。POSTGRES_* 配置也不会自动改变旧数据卷中的账号、密码或数据库名称。

## Docker 操作

```powershell
docker compose -f docker/docker-compose.yml up -d --wait
docker compose -f docker/docker-compose.yml ps
docker compose -f docker/docker-compose.yml down
```

down 停止容器并保留数据卷。如果确实需要**删除本项目测试数据并重新初始化**，使用 `docker compose -f docker/docker-compose.yml down -v`，再运行 up。此命令会删除这个 Compose 项目的 pgdata 卷。

连接失败时先确认 Docker 引擎已运行、容器 healthy、端口和 DB_* 配置一致。JDBC 连接超时 5 秒、读取超时 15 秒、SQL 查询超时 10 秒。

## 报告、截图和故意失败探针

业务报告默认保存在 target/surefire-reports。可指定独立报告目录：

```powershell
.\mvnw.cmd '-Dgroups=database' '-Dsurefire.reportsDirectory=target/database-reports' test
.\mvnw.cmd '-Dtest=DatabaseRollbackProbe' '-Dsurefire.reportsDirectory=target/db-probe-reports' test
```

第二条命令会插入 rollback-probe@example.test 后故意失败，预期 BUILD FAILURE；清理完成后新连接应查询不到该用户。它不在 testng.xml 或默认测试发现命名模式中，不计入 46 个业务场景。

Web 失败仍自动保存 test-output/screenshots 下的 PNG，成功不截图。API 在失败时输出脱敏请求/响应并清理自己创建的预约。历史 ScreenshotProbe 和 ApiFailureProbe 也仅显式运行，不属于业务套件。详见 V1/V2 README。

## 文档与交接

2026-09-13 实测数据库 8 个场景通过，完整 clean test 的 46 个场景全部通过（0 失败、0 错误、0 跳过）。故意失败后的独立连接检查确认探针数据已回滚，种子数据保持 1 用户、1 订单、2 明细。数据库容器当前保持运行。

- [V4 开发交接](docs/V4_HANDOFF.md)：新增模块、验收结果，供教学聊天读取。
- [V2 README](docs/V2_README.md)：14 个 API 场景、认证和日志详情。
- [V1 README](docs/V1_README.md)：24 个 Web 场景和 Page Object 架构。
- [V2 源码快照](docs/v2-baseline.zip)、[V1 快照](docs/v1-baseline.zip)、[V0 快照](docs/v0-baseline.zip)。
- 原阶段计划和跳过 V3 的决定见 QA_Automation_Codex_V0-V5_Plans/README_STAGE_ORDER.md。

V5 的实现和验收记录见下文。公开站点的目录、英文文案、API 行为发生变化仍可能导致外部测试失败。


## V5：跨浏览器、并行、报告与 CI

业务场景仍为 46 个。Chrome 与 Firefox 复用同一套 24 个 Web 场景，不把浏览器矩阵计作新增业务场景。V3 Appium 未开发，本项目不包含移动端测试。

```text
Web: Selenium → Page Objects → SauceDemo
API: REST Assured → Booking/Auth clients → Restful Booker
DB:  JDBC → prepared SQL + rollback → Docker PostgreSQL
                ↓
          TestNG + Maven
                ↓
      GitHub Actions / local run
                ↓
      Allure + Surefire + failure PNG
```

### 浏览器与并行

```powershell
.\mvnw.cmd '-Dgroups=web' '-Dbrowser=chrome' '-Dheadless=true' test
.\mvnw.cmd '-Dgroups=web' '-Dbrowser=firefox' '-Dheadless=true' test
.\mvnw.cmd -Pparallel-web '-Dbrowser=chrome' '-Dheadless=true' test
.\mvnw.cmd -Pparallel-web '-Dbrowser=firefox' '-Dheadless=true' test
```

默认 Chrome、有窗口、串行。Selenium Manager 管理驱动，Firefox 未安装时可自动下载浏览器，需要联网。`-Dheadless` 优先于环境变量 `HEADLESS`，只接受 true/false。

`parallel-web` 选择 testng-web-parallel.xml，两个工作线程按类并行，只运行 Web。默认 testng.xml 保持全部模块串行。可以给并行命令加 `-Dgroups=smoke` 选择 Web smoke 子集。

BaseTest 用 ThreadLocal 保存当前线程的 driver 和 LoginPage，每次测试新建浏览器，finally 中 quit/remove；Page Object 不使用静态 driver。API 和数据库仍按串行生命周期设计，不支持擅自切换 methods 或 DataProvider 并行。

### Allure

```powershell
# 新回归前 clean 避免历史结果混入，需要先启动数据库
.\mvnw.cmd '-Dheadless=true' clean test
.\mvnw.cmd allure:report
# 启动本地服务查看交互报告，Ctrl+C 停止
.\mvnw.cmd allure:serve
```

原始结果 target/allure-results，HTML target/site/allure-maven-plugin。推荐 allure:serve 查看，而不是双击 index.html。首次使用插件会下载 Allure CLI。无需全局 Allure 或 AspectJ：使用编程方式添加附件，没有 @Step/@Attachment 注解。

报告包含状态、断言堆栈、参数、分组标签，以及 Web 的 browser/headless 标签。失败 PNG 在浏览器关闭前保存并附加到 Allure，成功不截图。登录密码参数在 Allure 原始 JSON 中替换为 [REDACTED]。Surefire 本身仍可能记录参数；此演示项目只使用公开测试凭据，不要传入生产凭据。

故意失败探针应单独运行，避免污染业务报告：

```powershell
.\mvnw.cmd '-Dtest=ScreenshotProbe' '-Dheadless=true' '-Dallure.results.directory=target/probe-allure-results' '-Dsurefire.reportsDirectory=target/probe-reports' test
```

预期 2 失败、1 通过，Maven 返回非零；两条失败结果应各有 PNG 附件。不计入 46 个业务场景。

### GitHub Actions

.github/workflows/qa-tests.yml 响应 push、pull_request、手动触发。四个独立 job：Chrome Web、Firefox Web、API、数据库。Java 21 + Maven 缓存；Web 无头按类并行；数据库 job 启动并等待 Compose 健康。

测试失败后仍尝试生成 Allure HTML、上传 Surefire/Allure/截图；数据库另存日志并停止容器。失败不会通过自动重试或忽略退出码变绿。产物保留 14 天。

当前目录未初始化 Git、没有 GitHub 远端，工作流只完成本地静态校验，**尚未在 GitHub Actions 实际运行**。上传仓库后须核对四个 job 和可下载产物，才能关闭云端验收项。没有伪造 CI 截图。

- [V5 开发交接](docs/V5_HANDOFF.md)：本次修改和实测记录。
- [V4 README](docs/V4_README.md) / [V4 源码快照](docs/v4-baseline.zip)：供版本对照。


## 项目配套教材

[打开 19 章网页教材](docs/textbook/index.html)：面向会 Java 基础的学习者，中英结合讲解测试思想、框架、进阶语法和当前源码，附练习答案与完整源码索引。可直接用浏览器离线打开。另有 [Markdown 合集](docs/textbook/QA_AUTOMATION_TEXTBOOK.md)。
