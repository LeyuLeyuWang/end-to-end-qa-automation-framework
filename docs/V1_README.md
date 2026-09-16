# QA Automation Framework — V1

基于 Java、Selenium、TestNG、Maven 的 SauceDemo Web 自动化学习项目。V1 将 V0 的直接 Selenium 脚本重构为 Page Object Model，并增加数据驱动、测试分组、显式等待工具和失败截图。

**业务套件：15 个 `@Test` 方法，DataProvider 展开后 24 个执行场景。** Chrome 可见窗口、顺序运行，每次执行使用独立浏览器会话。

## 环境和运行

需要 JDK 17+（本机使用 JDK 21）、Chrome 和可访问 SauceDemo、Maven Central、驱动下载站点的网络。配置 `JAVA_HOME` 后，在项目根目录运行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd '-Dgroups=smoke' test
.\mvnw.cmd '-Dgroups=negative' test
.\mvnw.cmd '-Dgroups=regression' test
.\mvnw.cmd '-Dtest=LoginTest' test
.\mvnw.cmd '-Dtest=LoginTest#standardUserCanLogin' test
```

安装了 Maven 也可以执行 `mvn test` 或 `mvn test -Dgroups=smoke`。Wrapper 固定 Maven 3.9.11，依赖版本见 `pom.xml`。macOS/Linux 入口为 `sh ./mvnw test`，当前验收平台为 Windows + Chrome。

每个业务测试都属于 regression。smoke 包含登录成功、商品目录、添加商品、移除商品、成功结账，共 5 个场景；negative 包含 5 个登录异常和 4 个结账异常，共 9 个场景。分组不是独立测试的副本，不应把各组数量相加作为总数。

## 架构与职责

```text
src/main/java/
  config/ConfigManager.java       系统属性和公开演示默认配置
  driver/WebDriverFactory.java    创建并配置 Chrome
  pages/LoginPage.java            登录操作和错误状态查询
  pages/ProductsPage.java         商品列表、排序、购物车入口
  pages/CartPage.java             购物车查询、移除和结账入口
  pages/CheckoutPage.java         信息填写、订单概览、完成和取消
  pages/Product.java              演示商品的稳定 ID 与显示名称
  utils/WaitUtils.java            可见、可点击、URL、元素消失等待
  utils/ScreenshotUtils.java      写入 PNG 文件
src/test/java/
  base/BaseTest.java              每次执行的浏览器生命周期
  data/TestDataProvider.java      登录、排序、结账测试数据
  listeners/TestListener.java     失败后、清理前自动截图
  tests/                         四个业务测试类
  diagnostics/ScreenshotProbe.java 仅显式运行的截图验收探针
```

测试继承 BaseTest。Page Object 持有当前 WebDriver，只负责页面操作、同步与状态查询；TestNG 断言保留在测试类。没有通用 BasePage、全局静态 driver 或 Selenium 操作的万能包装层。

Selenium 现在用于 `src/main/java` 的页面对象，因此从 test scope 改为默认 compile scope。TestNG 仍然仅供测试代码使用。

## 场景清单

| 类 | 方法数 | 展开场景数 | 覆盖 |
|---|---:|---:|---|
| LoginTest | 2 | 6 | 成功、错误密码、锁定用户、空用户名、空密码、全部为空 |
| ProductTest | 3 | 5 | 目录、名称升降序、价格升降序 |
| CartTest | 6 | 6 | 添加一个、移除一个、添加多个、移除后保留其他商品、商品页移除、继续购物后保留状态 |
| CheckoutTest | 4 | 7 | 成功、缺少名字/姓氏/邮编、全部为空、金额核对、取消返回购物车 |
| 合计 | 15 | 24 | 保留 V0 的 10 个业务行为并扩展 |

原来的三个登录异常方法合并为一个参数化方法；原来的缺少名字场景合并进必填信息矩阵。每一行数据仍会独立执行 `@BeforeMethod` 和 `@AfterMethod`，不会共享浏览器状态。

排序不仅验证下拉框，还比较价格/名称顺序及数据保留情况。金额测试检查背包 29.99、车灯 9.99 的演示价格、商品合计、总额等于小计加显示税额；没有宣称验证真实税率规则。

## 等待与定位

优先 ID、`data-test` CSS 和语义明确的 class；没有 XPath、隐式等待或 `Thread.sleep`。`WaitUtils` 默认上限为 10 秒，导航加载上限为 30 秒。

排序操作等待实际列表达到目标排序状态，避免只等待下拉框选中。排序缺陷因此可能先表现为页面方法中的 TimeoutException；测试仍独立断言排序结果、数量及数据完整性。它不是自动重试失败测试。

Page Object 保存定位器，读取时重新查找元素，不长期缓存 WebElement。页面动作返回对应的页面对象时，等待目标页的必要元素。

## 配置

| JVM 属性 | 默认值 |
|---|---|
| baseUrl | https://www.saucedemo.com/ |
| username | standard_user |
| password | secret_sauce |
| browser | chrome |
| waitSeconds | 10 |

例如：

```powershell
.\mvnw.cmd '-DwaitSeconds=15' '-Dgroups=smoke' test
```

这是公开演示账号。配置覆盖用于调试，不代表套件可测试任意网站；用例仍针对 SauceDemo 的页面、文案和目录。V1 只接受 Chrome，其他 browser 值会明确报错。

## 失败截图

BaseTest 注册 `TestListener`。监听器使用 `IInvokedMethodListener.afterInvocation`，在测试失败后立即截图，此时 `@AfterMethod` 尚未关闭浏览器；初始化失败时，若已有可用 driver，也尝试截图。

PNG 保存到 `test-output/screenshots/`。名称包含类名、方法名、毫秒时间戳和 UUID，因此 DataProvider 多次失败不会互相覆盖。路径写入 TestNG 输出和结果属性。成功测试不截图，截图失败不覆盖原始测试异常。创建浏览器之前失败时无法截图。

可显式运行诊断探针：

```powershell
.\mvnw.cmd '-Dtest=ScreenshotProbe' '-Dsurefire.reportsDirectory=target/screenshot-probe-reports' test
```

**这个命令预期 BUILD FAILURE：两个参数化执行故意断言失败，另一个执行通过。** 应新增两张不同 PNG，通过的执行不产生截图。探针不在 `testng.xml` 中，类名也不匹配默认 `*Test` 等发现规则，不计入 24 个业务场景。

## 报告和限制

业务报告位于 `target/surefire-reports/`。检查失败时先区分环境/初始化失败、定位或等待失败、业务断言失败，再结合截图定位。不要通过跳过失败或放宽预期来让报告变绿。

当前公共演示目录假设有 6 个商品，英文文案及演示商品价格保持不变。仅验证 Web UI 行为，没有访问真实订单数据库或支付服务。没有增加 API、Appium、SQL、Docker、CI、Allure、并行或跨浏览器功能。

## 学习材料

- [V1 中文重构讲解](docs/V1_WALKTHROUGH.md)：建议从精简后的 LoginTest 反向追踪调用。
- [V0 中文讲解](docs/V0_WALKTHROUGH.md) 与 [V0 README](docs/V0_README.md)：历史材料，部分方法名在 V1 已调整。
- [V0 源码快照](docs/v0-baseline.zip)：重构前四个测试类、配置和 Maven/TestNG 配置，解压到另一个目录对照阅读。
- 原始阶段计划保存在 `QA_Automation_Codex_V0-V5_Plans/`。

## V1 验收记录

2026-09-10 在本机 Windows、JDK 21.0.2、Chrome 环境实测：

| 验证 | 结果 |
|---|---|
| `clean test` 完整业务套件 | 24 执行，0 失败，0 错误，0 跳过 |
| 报告目录修正后 `-Dgroups=regression` | 24 执行，全部通过，业务报告与诊断报告分开保存 |
| `-Dgroups=smoke` | 5 执行，全部通过 |
| `-Dgroups=negative` | 9 执行，全部通过 |
| `-Dtest=ScreenshotProbe` | 3 执行，其中 2 次按设计故意失败、1 次通过；生成 2 张独立 PNG |

已实际打开失败截图确认登录页面可读。截图探针报告保存在独立目录 `target/screenshot-probe-reports/`，不会覆盖业务报告。截图和构建产物已由 `.gitignore` 排除。

详细本地命令日志保存在 `.tools/v1-*.log`（不提交版本管理）。
