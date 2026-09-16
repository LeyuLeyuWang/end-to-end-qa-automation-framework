# QA Automation Framework — V0

这是逐版本学习的 QA 自动化作品。目前只实现 V0：使用 Java、Selenium、TestNG 和 Maven 测试 [SauceDemo](https://www.saucedemo.com/) 的 Web 流程。

项目本身是测试代码，不包含电商应用，也不访问 SauceDemo 的数据库。

## 环境与运行

- 安装 JDK 17 或更高版本，配置 `JAVA_HOME`，确认 `java -version` 可用。
- 安装 Google Chrome；测试会打开独立的可见浏览器窗口。
- 首次运行需要联网下载 Maven、依赖和浏览器驱动；运行期间需要访问 SauceDemo。
- 已提供官方 Maven Wrapper，未安装全局 Maven 也可以运行。

在项目根目录使用 PowerShell：

```powershell
.\mvnw.cmd test
```

已安装 Maven 时：

```powershell
mvn test
```

只运行登录测试，或一个方法：

```powershell
.\mvnw.cmd '-Dtest=LoginTest' test
.\mvnw.cmd '-Dtest=LoginTest#standardUserCanLogin' test
```

macOS/Linux 可以使用 `sh ./mvnw test`，但本阶段以 Windows + Chrome 实测为准。

Selenium Manager 自动解析并下载匹配的驱动，无须在代码中硬编码 ChromeDriver 路径。参见 [Selenium 官方文档](https://www.selenium.dev/documentation/selenium_manager/)。首次下载较慢不代表用例已经失败。

## 目录与职责

```text
pom.xml                          依赖版本、Java 编译目标、测试执行插件
testng.xml                       本阶段的四个测试类
mvnw / mvnw.cmd / .mvn/          Maven Wrapper
src/test/java/tests/
  TestConfig.java               网站地址和公开演示账号
  LoginTest.java                登录测试，4 个
  ProductTest.java              商品展示和排序，2 个
  CartTest.java                 加入和移除商品，2 个
  CheckoutTest.java             成功结账和必填校验，2 个
docs/V0_WALKTHROUGH.md           中文代码讲解和面试表达
QA_Automation_Codex_V0-V5_Plans/  原始阶段规划
```

依赖及插件版本固定在 `pom.xml`；Wrapper 固定 Maven 3.9.11。

## 10 个测试场景

| 测试方法 | 验证结果 |
|---|---|
| standardUserCanLogin | 登录后 URL 与 Products 标题正确 |
| invalidPasswordIsRejected | 密码错误提示，仍停留在登录页 |
| emptyUsernameIsRejected | 用户名必填提示 |
| lockedOutUserIsRejected | 账号锁定提示 |
| productPageShowsCatalog | 标题、6 个商品、背包商品存在 |
| priceSortChangesProductOrder | 价格按数字升序排列，顺序确实改变 |
| addedProductAppearsInCart | 徽标、商品名称和数量正确 |
| removedProductDisappearsFromCart | 商品与购物车徽标消失 |
| customerCanCompleteCheckout | 确认商品后完成结账，成功提示和 URL 正确 |
| missingFirstNamePreventsCheckout | 缺少名字显示错误，不能进入确认步骤 |

这里的 10 个测试指 10 个 `@Test` 方法，没有参数化膨胀或覆盖率推算。

## 设计约定

- 每个测试通过 `@BeforeMethod` 新建浏览器，通过 `@AfterMethod(alwaysRun = true)` 清理，即使断言失败也执行清理。
- 使用 ID 和 CSS（包括 `data-test`）定位，不使用基于 DOM 位置的 XPath。
- 在页面转换和动态结果处直接使用最长 10 秒的显式等待，没有 `Thread.sleep` 或隐式等待。
- 四个类保留少量重复初始化和登录代码，方便 V0 阅读；V1 再引入 BaseTest、Page Object 和等待工具。
- Chrome 密码保存及泄漏检测提示关闭，避免公开演示密码触发浏览器弹窗干扰流程。
- 本阶段顺序执行，只支持 Chrome；未加入 API、移动端、数据库、CI 或 Allure。

## V0 实测结果

2026-09-10，在 Windows 11、JDK 21.0.2、Chrome 152.0.7977.83、Maven 3.9.11 环境执行完整 `test` 阶段：**10 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS**。测试套件耗时 79.18 秒（包含首次驱动准备，不作为性能指标）。

另外通过 `mvnw.cmd` 单独执行 `LoginTest#standardUserCanLogin` 成功，验证了 Wrapper 和单方法运行入口。测试结束后未发现遗留 ChromeDriver 进程。

## 报告与排错

控制台末尾查看 `Tests run`、`Failures`、`Errors`、`Skipped` 和 `BUILD SUCCESS`。测试报告保存在 `target/surefire-reports/`，该目录不提交版本管理。

- `JAVA_HOME` 错误：确认它指向 JDK 根目录，而不是 `bin`。
- 驱动下载失败：检查访问驱动下载站点的网络条件，以及 Chrome 是否可用。
- `TimeoutException`：检查网站是否可访问、页面元素是否变更，再查看失败位置。
- `AssertionError`：比较期望值和实际值；不要为了通过测试随意放宽断言。

公共站点故障、页面改版、账号或商品变化都可能导致失败。当前假设公开账号 `standard_user / secret_sauce` 可用，商品目录有 6 项，英文提示和背包商品保持稳定。所有下单均在演示站点进行。

每阶段验收后先讲解再继续，完整路线见原始规划目录。
