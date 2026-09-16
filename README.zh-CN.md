# End-to-End QA Automation Framework

> 现已支持 Jenkins，详见 [流水线与本地配置说明](docs/JENKINS.md)。保留 GitHub Actions；本地 Jenkins 任务手动触发。

[English](README.md) | **简体中文**

Java QA 自动化作品集，涵盖浏览器流程、REST API 和数据库验证。项目结合可复用的测试组件、独立测试数据、失败诊断、跨浏览器执行和 GitHub Actions 工作流。

**46 个业务场景执行 · 35 个测试方法 · Chrome 与 Firefox · Allure 报告**

## 测试范围

| 模块 | 被测目标 | 场景数 | 主要内容 |
|---|---|---:|---|
| Web UI | SauceDemo | 24 | 登录校验、商品排序、购物车状态、结账与显示金额 |
| REST API | Restful Booker | 14 | 认证、增删改查、部分更新、筛选与非法修改拒绝 |
| 数据库 | 本地 PostgreSQL fixture | 8 | 插入、更新、删除、关联查询、金额汇总、外键和参数绑定 |

这三个目标是**互相独立的系统**，不是同一产品的前端、接口和数据库。本地 PostgreSQL 不连接 SauceDemo 或 Restful Booker 的后端；Web 结账不验证真实支付。未实现移动端/Appium 测试。

46 次执行包含 DataProvider 参数展开。同一批 Web 场景在两种浏览器运行，不计作新增业务场景。故意失败探针不计入该数量。

## 工程设计

- **页面对象模式：** 页面定位和操作与业务断言分离。
- **显式等待：** 等待可观察页面状态，避免固定休眠。
- **Web 隔离：** 每次测试新建浏览器，driver 和页面引用按线程隔离。
- **API 数据所有权：** 创建唯一标记的预约，记录并清理自己拥有的数据，清理失败保持可见。
- **数据库隔离：** 每次测试使用独立 JDBC 连接，结束后回滚事务。
- **有效断言：** 被拒绝的 API 修改不能改变原数据，PATCH 必须保留未指定字段。
- **失败证据：** 浏览器关闭前截图并附加到 Allure；API 失败日志省略认证请求/响应体及请求头。
- **受控并行：** Web 按类使用两个线程；API 和数据库在各自套件中仍为串行。

## 架构与技术栈

```text
Web 测试 ── 页面对象 ── Selenium ─────────── SauceDemo
API 测试 ── API 客户端 ── REST Assured ──── Restful Booker
DB 测试 ─── DatabaseClient ── JDBC ─────── Docker PostgreSQL
                         │
                    TestNG / Maven
                         │
                本地运行 / GitHub Actions
                         │
              Surefire / Allure / 失败截图
```

Java 编译目标为 17，本地 V5 验收使用 JDK 21。依赖与插件版本固定在 [pom.xml](pom.xml)，包括 Selenium、TestNG、REST Assured、Jackson、pgJDBC、Allure 和 Maven Wrapper。PostgreSQL 镜像固定在 [Docker Compose](docker/docker-compose.yml)。

```text
src/main/java/pages/       页面操作和页面观察
src/main/java/api/         HTTP 客户端、模型、配置和诊断
src/main/java/database/    JDBC 连接与参数化 SQL 工具
src/test/java/tests/       Web 业务测试
src/test/java/api/         API 测试与数据生命周期
src/test/java/database/    数据库测试与事务生命周期
docker/                   表结构、种子数据和 Compose 配置
.github/workflows/        GitHub Actions 工作流
docs/                     版本交接、源码快照和学习教材
```

## 快速开始

需要 JDK 17+、`JAVA_HOME`；Web 测试需要 Chrome 或 Firefox；数据库测试需要支持 Linux 容器的 Docker。首次运行需联网下载依赖、驱动和镜像。仅运行 Web/API 不需要 Docker。

克隆后在项目根目录执行：

```powershell
git clone https://github.com/LeyuLeyuWang/end-to-end-qa-automation-framework.git
cd end-to-end-qa-automation-framework

# 仅 Web，不需要数据库
.\mvnw.cmd '-Dgroups=web' '-Dheadless=true' test

# 完整回归，先启动数据库
docker compose -f docker/docker-compose.yml up -d --wait
.\mvnw.cmd '-Dheadless=true' clean test

# 生成和查看 Allure 报告
.\mvnw.cmd allure:report
.\mvnw.cmd allure:serve
```

Linux/macOS 使用 `sh ./mvnw` 替代 `.\mvnw.cmd`，例如：

```bash
sh ./mvnw -Dgroups=web -Dheadless=true test
```

默认 Chrome、有窗口、串行。Selenium Manager 管理驱动；支持的浏览器自动下载需要联网。本地实际验收平台为 Windows。

## 分组与跨浏览器运行

```powershell
.\mvnw.cmd '-Dgroups=api' test
.\mvnw.cmd '-Dgroups=database' test
.\mvnw.cmd '-Dgroups=smoke' '-Dheadless=true' test
.\mvnw.cmd '-Dgroups=negative' '-Dheadless=true' test
.\mvnw.cmd '-Dgroups=web' '-Dbrowser=firefox' '-Dheadless=true' test

# 仅 Web 类，两个工作线程
.\mvnw.cmd -Pparallel-web '-Dbrowser=chrome' '-Dheadless=true' test
.\mvnw.cmd -Pparallel-web '-Dbrowser=firefox' '-Dheadless=true' test
```

| 分组 | 展开执行数 |
|---|---:|
| `web` | 24 |
| `api` | 14 |
| `database` | 8 |
| `regression` | 46 |
| `smoke` | 9 |
| `negative` | 18 |

分组标签有重叠，不要相加。完整套件的 smoke 和 negative 都包含数据库测试。`parallel-web` 选择仅包含 Web 类的 [testng-web-parallel.xml](testng-web-parallel.xml)，再加 `-Dgroups=smoke` 只运行 Web smoke。API/DB 未重新设计共享状态前，不应直接启用方法或 DataProvider 并行。

## 配置与数据库 fixture

| 配置 | 默认值 | 覆盖方式 |
|---|---|---|
| 浏览器 | `chrome` | `-Dbrowser=chrome` 或 `firefox` |
| 无头模式 | `false` | 优先 `-Dheadless=true`，其次环境变量 `HEADLESS` |
| Web 地址 | `https://www.saucedemo.com/` | `-DbaseUrl=...` |
| 显式等待 | 10 秒 | `-DwaitSeconds=...` |
| API 地址 | `https://restful-booker.herokuapp.com` | `-DapiBaseUrl=...` |
| 数据库主机/端口 | `localhost` / `55432` | `DB_HOST` / `DB_PORT` |
| 数据库名 | `qa_fixture` | `DB_NAME` |
| 数据库用户/密码 | `qa_local` / `qa_local_only` | `DB_USER` / `DB_PASSWORD` |

Java 数据库配置优先级：系统属性 `-DDB_*` → shell 环境变量 → 默认值。建议使用 shell 变量同时配置 Compose 和 Java；Maven 属性不会修改容器端口映射。复制 [.env.example](.env.example) **不会**让 Java 自动读取 `.env`。只使用演示/测试凭据：部分 Allure/API 字段虽已脱敏，Surefire 仍可能记录测试参数。

本地表为 `users`、`orders`、`order_items`，初始化 1 个用户、1 个订单、2 个明细，金额合计 39.98。SQL 使用 `PreparedStatement`，金额使用 `BigDecimal`。每个测试回滚自己的事务；生成的序列 ID 不要求连续。

Compose 项目名保留为引入数据库模块时的 `qa-automation-v4`，端口只绑定 `127.0.0.1`。初始化脚本只在空数据卷上执行；重启旧卷不会重新初始化或自动迁移表结构。

```powershell
docker compose -f docker/docker-compose.yml ps
docker compose -f docker/docker-compose.yml down
```

`down` 保留命名卷。只有明确要**删除本项目 fixture 数据**时才使用 `down -v` 后重新启动。连接失败时检查 Docker 引擎、容器健康状态、端口和一致的 `DB_*` 配置。表结构与八个数据库场景的详细解释见 [V4 参考文档](docs/V4_README.md)。

## 报告与故意失败探针

| 产物 | 路径 |
|---|---|
| Surefire 结果 | `target/surefire-reports/` |
| Allure 原始结果 | `target/allure-results/` |
| Allure HTML | `target/site/allure-maven-plugin/` |
| Web 失败截图 | `test-output/screenshots/` |

使用 `clean test` 生成新一轮 Allure 结果，或指定独立结果目录。`allure:report` 生成 HTML，`allure:serve` 提供交互式浏览。首次由 Maven 下载 Allure CLI，无需全局安装 Allure 或配置 AspectJ。Maven `clean` 不会清理独立的截图目录。

探针用于显式验证失败处理，不属于业务套件：

```powershell
.\mvnw.cmd '-Dtest=ScreenshotProbe' '-Dheadless=true' '-Dallure.results.directory=target/probe-allure-results' '-Dsurefire.reportsDirectory=target/probe-reports' test
```

预期 **2 个故意失败、1 个通过**，两个失败结果各有独立 PNG 附件，Maven 非零退出码符合预期。`ApiFailureProbe` 和 `DatabaseRollbackProbe` 同样验证故意失败后的清理，需要显式选择，并使用独立报告目录。

## GitHub Actions 与验收记录

[QA 工作流](.github/workflows/qa-tests.yml) 响应 push、pull request 和手动触发，矩阵定义四个 job：Chrome Web、Firefox Web、API、数据库。配置 Java 21 和 Maven 缓存，Web 无头按类并行，数据库 job 等待 PostgreSQL 健康后运行。

测试失败后仍尝试生成 Allure、上传报告、截图和数据库日志，产物保留 14 天。不通过自动重试或忽略退出码把失败变成成功。

**V5 本地验收记录（2026-09-14）：** 完整套件 46/46 通过；Chrome 与 Firefox 无头 Web 并行各 24/24 通过；截图探针附件符合预期。工作流通过本地 actionlint 静态检查。这些历史记录不代表后续每次云端运行都通过，最新状态请查看 [GitHub Actions](https://github.com/LeyuLeyuWang/end-to-end-qa-automation-framework/actions)。

公共演示站点、网络和上游页面/API 变化可能影响结果。本项目不宣称已测量代码覆盖率、性能提升或完整安全覆盖。

## 教材与版本历史

- [教材说明](docs/textbook/README.md)：19 章，中文讲解结合英文术语和面试表达，覆盖测试思想、框架、Java 语法和项目源码。
- [在线阅读 Markdown 合集](docs/textbook/QA_AUTOMATION_TEXTBOOK.md)，或克隆后用浏览器打开 `docs/textbook/index.html`。GitHub 上的 HTML 文件页不等于已托管的教材网站。
- [V5 交接](docs/V5_HANDOFF.md)、[V4 参考](docs/V4_README.md)、[V2 参考](docs/V2_README.md)、[V1 参考](docs/V1_README.md)。
- 源码快照：[V0](docs/v0-baseline.zip)、[V1](docs/v1-baseline.zip)、[V2](docs/v2-baseline.zip)、[V4](docs/v4-baseline.zip)。
- 开发路线：**V0 → V1 → V2 → V4 → V5**。V3 移动端按决定暂缓，历史文档描述对应版本的快照状态。
