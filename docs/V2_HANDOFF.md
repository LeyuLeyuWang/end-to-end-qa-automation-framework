# V2 开发交接

## 本次范围

在 V1 Web 框架上添加 Restful Booker API 测试，不改动原来 24 个 Web 场景的业务断言。Web 测试增加 web 标签，API 使用 api 标签，testng.xml 默认执行两类套件。V1 源码及 README 已归档。

主代码新增 api 包中的配置、请求构建、诊断过滤器、AuthClient、BookingClient 和三个 Java record；测试代码新增 BaseApiTest、BookingData、AuthApiTest、BookingApiTest。客户端不依赖 TestNG，断言集中在测试层。

共 14 个 API 场景（12 个测试方法，其中无认证修改按 3 种 HTTP 方法参数化）。覆盖创建、查询、PUT、PATCH、DELETE、认证失败、无效 token、缺失请求体、404 和查询过滤。

## 隔离与诊断

每次测试独立创建客户端和 trace；token 按测试获取，创建前准备清理凭据。UUID firstname 作为所有权标记，登记自己的 ID，测试结束检查所有权、删除、验证 404。测试不删除公共固定 ID 的预约。

不记录请求头和 Cookie，认证请求与响应正文始终隐藏。其他 JSON 仅在失败后输出，便于分析字段与状态码；正常运行不输出请求日志。不自动重试和跳过外部服务异常。

ApiFailureProbe 是显式运行的故意失败探针，用于验证清理和日志，不属于业务用例。它在 target/api-probe-id.txt 保存创建的 ID，便于运行后独立检查服务已返回 404。

初次验收发现 ContentType.JSON 用于 Accept 时与 demo 内容协商不兼容，创建返回 418。修改为显式 `application/json`，并把状态码验证放到创建响应的 JSON 解析之前，保留清晰故障信息。

## 给教学聊天的材料

提供 src/main/java/api、src/test/java/api、pom.xml、testng.xml 和当前 README 即可。建议阅读顺序：Booking 模型 → BookingClient / AuthClient → ApiRequests → 单个 API 测试 → BaseApiTest 的清理 → ApiTrace。已有 Java 和 V1 知识无需重新讲解。

REST Assured 6.0.1 使用 Jackson 2.22.2，显式选择 JACKSON_2；没有 Spring 或后端应用。这里只验证公开演示 API，不是 SauceDemo 的真实后台。

## 运行

```powershell
.\mvnw.cmd '-Dgroups=api' test
.\mvnw.cmd '-Dgroups=web' test
.\mvnw.cmd test
```

当前路线 V0 → V1 → V2 → V4 → V5，V3 按用户决定暂缓。代码开发和验收由此任务处理，教学由另一个聊天处理。

## 实测验收（2026-09-13）

- API-only：14 个执行，0 失败、0 错误、0 跳过，BUILD SUCCESS。
- `clean test`：38 个执行，0 失败、0 错误、0 跳过，BUILD SUCCESS；包含原有 24 个 Web 场景，套件耗时 92.32 秒，仅记录本次运行，不作为性能指标。
- ApiFailureProbe：1 次按设计故意失败；随后独立 HTTP GET 确认创建的 ID 返回 404，证明失败后的清理执行。
- 探针日志有 `[REDACTED]`，未出现原始 token/password JSON 字段；包含创建、查询、删除及最终 404 的诊断。
- 验收后未发现残留 ChromeDriver 进程。
- 完整业务报告：`target/surefire-reports/`；故意失败探针报告：`target/api-probe-reports/`，两者独立。
- 命令日志：`.tools/v2-api.log`、`.tools/v2-full.log`、`.tools/v2-api-probe.log`，均不提交版本管理。
