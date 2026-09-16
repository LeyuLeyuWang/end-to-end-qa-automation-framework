# V0 讲解：一个自动化测试怎样运行

> 这是 V0 历史讲解。当前源码已进入 V1；原始文件见同目录的 v0-baseline.zip，当前讲解见 V1_WALKTHROUGH.md。

## 1. 四个工具分别做什么

Java 用来编写测试逻辑。Selenium 控制浏览器：找元素、输入、点击、读页面。TestNG 组织测试生命周期并执行断言。Maven 下载依赖、编译代码，通过 Surefire 插件调用 TestNG。

执行 `./mvnw.cmd test` 时，Wrapper 先准备指定版本的 Maven，Maven 读取 `pom.xml`，编译 `src/test/java`，Surefire 根据 `testng.xml` 运行四个测试类，最后输出报告。

## 2. 从 LoginTest 看完整流程

先看 `openBrowser()`：它有 `@BeforeMethod`，因此每个测试方法之前都会执行。`WebDriver` 是浏览器控制接口，`ChromeDriver` 是控制 Chrome 的实现。将变量声明为 `WebDriver`，可以通过统一接口使用浏览器功能。

Chrome 使用独立的自动化会话。窗口尺寸固定，页面加载超时为 30 秒，等待某个元素或 URL 的时间上限为 10 秒；这是两种不同的超时。

再看 `standardUserCanLogin()`：

```java
submitLogin(TestConfig.VALID_USERNAME, TestConfig.VALID_PASSWORD);
wait.until(ExpectedConditions.urlContains("/inventory.html"));
assertTrue(driver.getCurrentUrl().endsWith("/inventory.html"), "Login should open inventory");
assertEquals(driver.findElement(By.cssSelector("[data-test='title']")).getText(), "Products");
```

`submitLogin` 找到输入框，输入公开账号密码，再点击按钮。显式等待给页面跳转留出时间；URL 断言确认跳到了目标页面，标题断言确认看到了商品页面内容。点击操作成功并不等于业务结果正确，所以测试必须有断言。

最后看 `closeBrowser()`：它有 `@AfterMethod(alwaysRun = true)`。测试失败也应该释放资源；`quit()` 关闭整个 WebDriver 会话，而不仅是当前标签页。若创建驱动失败，变量为 null，就不执行清理。

## 3. 为什么每个测试都重新登录

测试应能单独运行，不能依赖“前一个测试已经登录或加过购物车”。新浏览器让 cookie、登录状态和购物车互不干扰。代价是执行更慢，但 V0 只有 10 个测试，优先保证清晰和独立。

## 4. 为什么不用固定等待

`Thread.sleep(3000)` 无论页面多快都停 3 秒，页面超过 3 秒又可能失败。显式等待会反复检查具体条件，条件满足就继续，超过上限才失败。它不会让失效的定位器自动变正确。

## 5. 商品排序测试为什么转换价格

页面价格是带 `$` 的字符串。字符串排序可能把 `100` 放在 `20` 前面，因此先去掉货币符号，再转成 `BigDecimal` 比较数值。测试检查完整价格列表是否升序，同时检查商品数量不变、顺序确实不同于默认顺序，避免只验证下拉框选中了一个选项。

## 6. 为什么现在还有重复代码

V0 先展示最直接的 Selenium 工作流。四个类里重复的浏览器生命周期，是下一阶段引入 BaseTest 的具体动机；登录步骤和定位器的重复，是引入 Page Object 的动机。这里先观察问题，再理解抽象的价值。

## 7. 面试可以这样讲

“我先用 Java、Selenium 和 TestNG 建立了 SauceDemo 的 Web 自动化基线，覆盖 10 个登录、商品、购物车和结账场景。每个测试独立创建浏览器，并在结束时清理，减少状态依赖。我用 ID 和 data-test 属性定位，用显式等待处理页面变化，断言 URL、错误信息和业务数据。当前是基础版本，后续会针对重复初始化和页面操作重构成 Page Object。”

能够进一步解释的三个问题：

- 测试点了登录按钮但没有断言，能证明登录成功吗？不能，只证明交互步骤没有抛异常。
- 排序测试只检查下拉框文字够吗？不够，必须检查商品结果是否真的按预期排序。
- 测试失败是否意味着产品有 bug？不一定，还要排查环境、定位器、数据和预期是否正确。

## 8. 建议阅读顺序

先读 `TestConfig.java`，再读 `LoginTest.java`，然后看购物车和结账，最后看商品排序。理解测试后，再回头看 `pom.xml` 和 `testng.xml` 如何把它们组织起来。
