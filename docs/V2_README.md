# QA Automation Framework — V2

Java 自动化测试学习项目：使用 Selenium 测试 SauceDemo Web UI，使用 REST Assured 测试 Restful Booker API。两个公开演示系统相互独立，不宣称 API 是 SauceDemo 的后台。

**业务套件：24 个 Web 场景 + 14 个 API 场景，共 38 个展开执行。** Web 保留 V1 的 Page Object、DataProvider、失败截图；API 使用独立客户端、结构化 Java record 模型和每次测试的数据清理。

## 运行

安装 JDK 17+，配置 JAVA_HOME。Web 需要 Chrome；API-only 不启动浏览器。首次运行联网下载 Maven、依赖及必要的浏览器驱动。

在项目根目录执行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd '-Dgroups=api' test
.\mvnw.cmd '-Dgroups=web' test
.\mvnw.cmd '-Dgroups=smoke' test
.\mvnw.cmd '-Dgroups=negative' test
.\mvnw.cmd '-Dtest=BookingApiTest' test
.\mvnw.cmd '-Dtest=LoginTest#standardUserCanLogin' test
```

已安装 Maven 时可用 `mvn test -Dgroups=api`。macOS/Linux 可用 `sh ./mvnw test`，当前实测平台为 Windows。Maven Wrapper 固定 3.9.11，REST Assured 6.0.1，Jackson 2.22.2，全部依赖版本在 pom.xml 中固定。

| 分组 | 展开执行数 |
|---|---:|
| web | 24 |
| api | 14 |
| regression（所有业务测试） | 38 |
| smoke（跨 Web/API） | 8 |
| negative（跨 Web/API） | 16 |

分组为重叠标签，不应相加作为总数。若想只运行 Web smoke，可显式选择 Web 测试类并指定 smoke：

```powershell
.\mvnw.cmd '-Dtest=LoginTest,ProductTest,CartTest,CheckoutTest' '-Dgroups=smoke' test
```

## API 架构

```text
src/main/java/api/
  ApiConfig.java             独立的 API URL、公开演示账号配置
  ApiRequests.java           每次请求的 JSON、超时和序列化配置
  ApiTrace.java              每次测试的请求/响应诊断缓冲
  AuthClient.java            POST /auth
  BookingClient.java         /booking CRUD、过滤查询
  models/
    AuthRequest.java
    Booking.java
    BookingDates.java
src/test/java/api/
  BaseApiTest.java           客户端初始化、token、数据登记和清理
  BookingData.java           唯一测试预约数据
  AuthApiTest.java           2 个认证测试
  BookingApiTest.java        10 个方法，展开为 12 个场景
```

客户端只返回 Response，不做断言。测试层负责状态码、Content-Type、完整字段比较和业务预期。Java record 是结构化 JSON 模型，由 Jackson 2 显式序列化/反序列化；只有部分更新和故意缺字段的请求使用 Map。

每个请求创建新的 RequestSpecification，不修改 RestAssured 静态全局配置。API 测试不继承 Web BaseTest，因此 API-only 无需 Chrome。

## API 覆盖

| 场景 | 检查 |
|---|---|
| 正确认证 | 200、JSON、非空 token |
| 错误认证 | 200、Bad credentials、无 token |
| 创建预约 | 200、JSON、正数 ID、返回全部字段 |
| 获取预约 | 200、JSON、与原始请求模型一致 |
| PUT 更新 | 更新响应及随后 GET 均匹配全部预期字段 |
| PATCH 更新 | 指定字段改变，其他字段保留；随后 GET 验证持久化 |
| 删除预约 | DELETE 201，随后 GET 404 / Not Found |
| 不存在的预约 | GET -1 返回 404；该 demo 生成正数 ID |
| 缺少必填请求体 | 空对象返回 500 / Internal Server Error，记录 demo 当前行为 |
| 无认证修改 × 3 | PUT、PATCH、DELETE 均 403，原始预约数据不变 |
| 无效 token | PATCH 403，原始预约数据不变 |
| 名称过滤 | GET /booking?firstname=...&lastname=... 包含本次创建的 ID |

共 12 个 API @Test 方法，DataProvider 展开后 14 个场景。Web 的 15 个方法展开为 24 个场景，总计 27 个业务方法、38 个执行。

## 认证、隔离和清理

- POST /auth 获取 token，修改/删除通过 `Cookie: token=...` 认证；不会在客户端自动附加认证，以便明确测试无认证行为。
- 每次测试生成 UUID firstname，只创建和修改自己的数据，不使用共享的硬编码预约 ID。
- 创建前获取清理所需 token；登记成功创建的 ID，`@AfterMethod(alwaysRun = true)` 查询 ownership 后删除并验证 404。
- 若预约已被测试删除，清理接受 404；若 firstname 被外部修改，拒绝删除并报告清理问题。
- 多个清理失败会保留诊断；已有测试异常时附加清理异常，否则让清理失败显式出现在报告中。
- 服务共享数据可能被外部访问或重置。测试失败不会自动重试或静默跳过；进程被强制终止时，无法保证清理执行。

## 日志和报告

API 每次请求记录方法、URL、请求体、响应状态、类型及正文（正文最多 2000 字符）。仅在测试或清理失败时输出到 TestNG 报告/控制台。认证接口的请求和响应正文全部隐藏，所有请求头和 Cookie 不记录；不要自行添加无过滤的 `.log().all()`。

连接和读取超时均为 15 秒，不用公共 demo 测响应性能指标。默认报告在 `target/surefire-reports/`，可指定独立目录：

```powershell
.\mvnw.cmd '-Dgroups=api' '-Dsurefire.reportsDirectory=target/api-reports' test
```

Web 测试失败时仍自动保存 `test-output/screenshots/` PNG，包含方法名、时间戳、UUID；成功测试不截图。V1 的 ScreenshotProbe 仍只可显式运行，故意失败，不计入业务套件。详见 [V1 README](docs/V1_README.md)。

API 失败路径也有显式诊断探针：

```powershell
.\mvnw.cmd '-Dtest=ApiFailureProbe' '-Dsurefire.reportsDirectory=target/api-probe-reports' test
```

该命令创建自己的预约后故意失败，预期 BUILD FAILURE；清理仍应删除预约，报告应输出预约请求/响应，但认证正文为 `[REDACTED]`。本次 ID 保存到 `target/api-probe-id.txt`，可用 GET /booking/{id} 验证 404。探针不在业务套件和默认测试命名规则内，不计入 38 个执行。

## 配置

| 属性 | 默认值 |
|---|---|
| apiBaseUrl | https://restful-booker.herokuapp.com |
| apiUsername | admin |
| apiPassword | password123 |
| baseUrl（Web） | https://www.saucedemo.com/ |
| username / password（Web） | standard_user / secret_sauce |
| browser | chrome，仅支持 Chrome |
| waitSeconds（Web） | 10 |

这些是公开演示账号，真实凭据不应写入代码或命令历史。改变 URL 不代表可以直接测试其他 API，客户端与测试仍针对 Booker 的契约。

## 已知服务行为与范围

Restful Booker 是测试用演示服务，DELETE 返回 201、错误认证返回 200，空预约请求体返回 500。测试明确记录这些实际行为，不将它们宣传为理想 REST 设计或已修复的产品缺陷。

请求的 Accept 明确设为 `application/json`。REST Assured 的 ContentType.JSON 枚举用于 Accept 时可能展开多个媒体类型，本服务在首次验收中返回了 418；改为明确的媒体类型后重新验证。API 的错误状态码先于 JSON 解析检查，避免解析异常掩盖响应错误。

V2 不包括性能测试、API 服务器、移动端、数据库、CI 或 Allure。下一步按已批准路线进入 V4，V3 Appium 暂缓。各阶段计划与最新决定见 `QA_Automation_Codex_V0-V5_Plans/README_STAGE_ORDER.md`。

## 文档与历史快照

2026-09-13 实测 API-only 14 个场景通过；完整 `clean test` 38 个场景全部通过，0 失败、0 错误、0 跳过。故意失败探针的预约在测试结束后独立查询为 404，日志脱敏检查通过。详见开发交接中的验收记录。

- [V2 开发交接](docs/V2_HANDOFF.md)：文件职责、验证结果和教学聊天所需材料。
- [V1 源码快照](docs/v1-baseline.zip) 与 [V1 讲解](docs/V1_WALKTHROUGH.md)。
- [V0 源码快照](docs/v0-baseline.zip) 与 [V0 讲解](docs/V0_WALKTHROUGH.md)。
- [Restful Booker 官方接口文档](https://restful-booker.herokuapp.com/apidoc/index.html)。
- [REST Assured 官方说明](https://rest-assured.io/)。
