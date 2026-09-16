# V1：从脚本到可维护的测试框架

你已经读过 V0。V1 的目标是保留原来的业务行为，让重复的准备和页面实现各自有明确归属。先不用一口气读所有新文件，按下面的调用链逐层看。

## 1. 先比较同一个登录测试

V0 的 LoginTest 自己创建浏览器、查找输入框、输入、点击、等待 URL，然后断言标题。

V1 的成功登录测试变为：

```java
@Test(groups = {"smoke", "regression"})
public void standardUserCanLogin() {
    assertTrue(loginAsStandardUser().isLoaded(), "Login should open the Products page");
}
```

操作没有消失，而是分配到了职责更明确的位置：

```text
BaseTest.openBrowser()
  → WebDriverFactory.createDriver()
  → LoginPage.open()
测试 standardUserCanLogin()
  → BaseTest.loginAsStandardUser()
  → LoginPage.login(...)
  → ProductsPage.awaitLoaded()
  → ProductsPage.isLoaded()
  → TestNG assertTrue(...)
BaseTest.closeBrowser()
```

理解重点：测试描述“标准用户可以进入商品页”，页面对象实现“如何在这个页面登录、如何读取页面状态”。

## 2. BaseTest 解决四份生命周期重复

四个测试类继承 BaseTest。TestNG 执行继承的 `@BeforeMethod(alwaysRun = true)` 和 `@AfterMethod(alwaysRun = true)`，每个测试调用（包括每一行 DataProvider）都使用新的浏览器。

`alwaysRun` 对准备方法也有意义：即使用 `-Dgroups=smoke` 筛选测试，通用准备仍应执行。BaseTest 的 driver 是实例字段；V1 顺序执行，没有宣称支持线程安全并行。以后并行化必须重新处理这个共享状态。

浏览器选项移到 WebDriverFactory，所有测试修改一次即可生效。工厂只负责创建可用 driver；若创建后的配置失败，会尝试清理并保留原异常。

## 3. Page Object 解决定位器和操作重复

例如 LoginPage 拥有 USERNAME、PASSWORD、LOGIN、ERROR 定位器和登录动作。测试不再使用 By、findElement、WebDriverWait。

`login()` 清空输入框再填写，因此同一页上的再次输入不会拼接旧值。它不强制断言登录成功，负向测试可以复用它。

`ProductsPage.openCart()` 返回 CartPage；`CartPage.checkout()` 返回 CheckoutPage。页面跳转在调用表达上也能看出来：

```java
var checkout = products.openCart().checkout();
```

构造页面对象不会自动操作网页，显式的 open/awaitLoaded 等方法负责进入或等待页面。页面对象不依赖 TestNG，不包含业务断言；读取状态后由测试决定预期。

CheckoutPage 包含结账信息、概览和完成三个步骤，V1 暂时保持一个小类。只有当它明显变大或步骤具有独立复用需求时，才值得继续拆分。

## 4. DataProvider 让相同流程使用多组数据

V0 三个失败登录方法只有账号、密码和错误提示不同。现在统一为：

```java
@Test(dataProvider = "rejectedLogins", dataProviderClass = TestDataProvider.class,
      groups = {"regression", "negative"})
public void invalidLoginIsRejected(String scenario, String username,
                                   String password, String expected) {
    loginPage.login(username, password);
    assertEquals(loginPage.errorMessage(), expected, scenario + ": incorrect rejection reason");
    assertTrue(loginPage.isDisplayed(), scenario + ": rejected login must remain on login page");
}
```

DataProvider 返回二维数组，每行对应一次方法调用，每列按参数顺序传入。scenario 是可读的案例标签，报告和失败信息更容易定位数据行。

五行登录异常数据产生五次测试执行，不是一次测试里循环五遍。某一行失败，其他行仍可以独立执行，也有自己的 setup/cleanup。结账必填校验、四种排序也使用相同机制。

业务总计 15 个 @Test 方法、24 次展开执行：登录 6，商品 5，购物车 6，结账 7。诊断探针不算业务覆盖。

## 5. Groups 是筛选标签

所有业务测试属于 regression。五个关键正常流程属于 smoke；九个输入异常场景属于 negative。一个测试可以有多个标签，选 smoke 不会复制测试。

```powershell
.\mvnw.cmd '-Dgroups=smoke' test
```

这个命令用来快速检查主要流程。negative 测试通过意味着网站正确拒绝错误输入，不意味着框架预期测试失败。

## 6. 等待移到了页面行为内部

WaitUtils 只提供几个常用条件，没有包装 Selenium 的全部 API。测试不需要了解“点击后应该等哪个 DOM 元素”，因为页面动作应该在必要的页面状态准备好后返回。

商品排序现在等实际列表达到目标顺序，再让测试核对排序前后的内容。好处是避免控件已经选中而商品尚未更新的竞态；代价是排序功能错误可能表现为页面同步超时，而不是最后的 assertEquals 失败。读取报告时要结合方法名和截图分析。

测试仍然独立建立 expected 列表，核对数据和数量，不会仅因页面方法返回就认为业务正确。没有自动重试失败测试，没有用 sleep 掩盖同步问题。

## 7. 截图最关键的是时机

如果 driver.quit() 后才截图，页面会话已经不存在。监听器在方法执行结束的 afterInvocation 回调里检查失败状态，立即截图；然后 TestNG 执行 AfterMethod 关闭浏览器。

```text
测试抛出 AssertionError
  → 监听器识别 FAILURE
  → ScreenshotUtils 写 PNG
  → 报告记录路径
  → BaseTest.closeBrowser()
```

带时间戳和 UUID 的文件名避免参数化用例覆盖同名文件。截图出现错误时仅记录诊断，不替换原始业务异常。

diagnostics/ScreenshotProbe 是显式启用的验收工具：同一个方法用两行数据故意失败，同时运行一个成功方法。预期只有两张截图、文件名不同、原始失败仍能看到。它不在业务套件里，正常运行不会故意失败。

## 8. 为什么 pom.xml 改了 scope

Page Object 位于 src/main/java，引用 Selenium。test scope 依赖不能用于主代码编译，所以 Selenium 改成默认 compile scope；TestNG 仍是 test scope，因为断言、注解和监听器都在 src/test/java。

不是“重构一定要改 scope”，而是代码目录与依赖使用位置发生了变化。

## 9. 建议带读顺序

1. LoginTest → BaseTest：先看测试如何变短、生命周期去了哪里。
2. LoginPage → ProductsPage：再看定位器、等待和跳转如何封装。
3. TestDataProvider：理解一行数据就是一次独立执行。
4. CartTest、CheckoutTest 及对应页面：对比 V0 原场景。
5. TestListener → ScreenshotUtils：理解失败时机和证据保存。
6. 最后看 ConfigManager、WebDriverFactory、WaitUtils 和 pom.xml。

面试表达可以围绕具体修改成本：V0 改用户名定位器需要找多个测试类；V1 改 LoginPage 一处即可。测试依旧负责判断结果，Page Object 没有把断言藏起来。架构的价值来自减少维护成本，而不是文件数量变多。
