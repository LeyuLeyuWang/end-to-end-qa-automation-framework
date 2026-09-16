# 04 跟踪一次登录：生命周期与页面对象

> 本章目标：把一行测试还原成完整执行过程，理解 setup、teardown、对象组合与 Page Object Model。

## 4.1 从最短的测试开始


<p class="source-ref">原代码 · <a href="../source.html#s-897c63f7a90c-L1" target="_blank" rel="noopener">src/test/java/tests/LoginTest.java · L1–L23</a></p>

```java
package tests;

import base.BaseTest;
import data.TestDataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class LoginTest extends BaseTest {
    @Test(groups = {"web", "smoke", "regression"})
    public void standardUserCanLogin() {
        assertTrue(loginAsStandardUser().isLoaded(), "Login should open the Products page");
    }

    @Test(dataProvider = "rejectedLogins", dataProviderClass = TestDataProvider.class,
            groups = {"web", "regression", "negative"})
    public void invalidLoginIsRejected(String scenario, String username, String password, String expected) {
        loginPage().login(username, password);
        assertEquals(loginPage().errorMessage(), expected, scenario + ": incorrect rejection reason");
        assertTrue(loginPage().isDisplayed(), scenario + ": rejected login must remain on login page");
    }
}


```


`standardUserCanLogin()` 只有一个断言，但绝不等于“只执行了一行操作”。计算参数 `loginAsStandardUser().isLoaded()` 时，先调用基类的登录助手，得到 ProductsPage，再调用 isLoaded，最后把 boolean 交给 assertTrue。方法链按表达式求值顺序运行，没有神秘的自动登录。

负向方法调用 loginPage().login 后不会强行创建商品页，这是有意的：登录动作可能成功也可能失败。页面对象的 login 返回 void，让测试自己决定预期路线。若 login 内部总是等待商品页，所有正确拒绝的测试也会超时。

## 4.2 BaseTest 把隐含步骤显式化


<p class="source-ref">原代码 · <a href="../source.html#s-80b0c1afb007-L1" target="_blank" rel="noopener">src/test/java/base/BaseTest.java · L1–L42</a></p>

```java
package base;

import config.ConfigManager;
import driver.WebDriverFactory;
import listeners.TestListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import pages.LoginPage;
import pages.ProductsPage;

@Listeners(TestListener.class)
public abstract class BaseTest {
    private final ThreadLocal<WebDriver> drivers = new ThreadLocal<>();
    private final ThreadLocal<LoginPage> loginPages = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void openBrowser() {
        drivers.set(WebDriverFactory.createDriver(ConfigManager.browser()));
        loginPages.set(new LoginPage(getDriver()).open());
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        try {
            if (getDriver() != null) getDriver().quit();
        } finally {
            drivers.remove();
            loginPages.remove();
        }
    }

    public WebDriver getDriver() { return drivers.get(); }
    protected LoginPage loginPage() { return loginPages.get(); }

    protected ProductsPage loginAsStandardUser() {
        loginPage().login(ConfigManager.username(), ConfigManager.password());
        return new ProductsPage(getDriver()).awaitLoaded();
    }
}

```


一次成功登录的完整路径如下：TestNG 准备执行测试；openBrowser 创建 WebDriver，把它放入当前线程的容器；创建 LoginPage 并 open；测试调用 loginAsStandardUser；输入配置中的用户名密码；点击按钮；ProductsPage.awaitLoaded 等商品出现；isLoaded 核对页面身份；断言通过；closeBrowser 退出会话并移除引用。

```text
@BeforeMethod
  └─ createDriver → LoginPage.open
@Test
  └─ loginAsStandardUser → LoginPage.login → ProductsPage.awaitLoaded
  └─ assertTrue(products.isLoaded())
监听回调：记录结果，失败时尝试截图
@AfterMethod
  └─ quit → ThreadLocal.remove
```

在源码中，drivers 与 loginPages 是 private，子类通过 getDriver/loginPage 访问。protected 让测试子类使用辅助方法，不把所有字段直接暴露。ThreadLocal 的详细原理在第 14 章；本章先把 get 看作“取当前执行线程的对象”。

`finally` 必须理解：不论 try 中 quit 是否抛异常，都尝试 remove，避免线程复用时保留旧引用。但这段代码不能保证外部浏览器进程在任何系统错误下都已退出，也没有把 quit 异常吞掉。它是明确的正常路径与异常路径资源管理。

## 4.3 Page Object 是页面操作的接口


<p class="source-ref">原代码 · <a href="../source.html#s-fc9f20693507-L1" target="_blank" rel="noopener">src/main/java/pages/LoginPage.java · L1–L45</a></p>

```java
package pages;

import config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtils;

public final class LoginPage {
    private static final By USERNAME = By.id("user-name");
    private static final By PASSWORD = By.id("password");
    private static final By LOGIN = By.id("login-button");
    private static final By ERROR = By.cssSelector("[data-test='error']");
    private final WebDriver driver;
    private final WaitUtils wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        wait = new WaitUtils(driver);
    }

    public LoginPage open() {
        driver.get(ConfigManager.baseUrl());
        wait.visible(USERNAME);
        return this;
    }

    public void login(String username, String password) {
        var usernameInput = wait.visible(USERNAME);
        usernameInput.clear();
        usernameInput.sendKeys(username);
        var passwordInput = wait.visible(PASSWORD);
        passwordInput.clear();
        passwordInput.sendKeys(password);
        wait.clickable(LOGIN).click();
    }

    public String errorMessage() {
        return wait.visible(ERROR).getText();
    }

    public boolean isDisplayed() {
        return !driver.getCurrentUrl().contains("/inventory.html")
                && driver.findElements(LOGIN).stream().anyMatch(element -> element.isDisplayed());
    }
}
```


LoginPage 持有 WebDriver 与 WaitUtils。通过构造器传入 driver，意味着页面对象使用已有浏览器，不创建另一个浏览器。ProductsPage 和 CartPage 也采用同样方式，因此导航中对象可以变，浏览器会话保持同一个。

`private static final By USERNAME` 保存的是定位规则，不是屏幕上的实际输入框。By 可以描述“找 id 为 user-name 的元素”，等到调用时 Selenium 才寻找匹配对象。把这种不变规则设为 static final 没有共享浏览器的问题；把 WebDriver 设为 static 则完全不同。

open 返回 this，让 `new LoginPage(driver).open()` 返回刚打开的同一个页面对象。这是 fluent API 的一种形式：便利来自返回类型，并不是点号自动知道下一步该做什么。

login 的动作逐个发生：等待用户名可见，clear 清空旧值，sendKeys 输入；对密码做同样处理；等待按钮可点击，click。clear 避免重复使用页面时文字追加。即便当前每次新浏览器，也让这个动作方法本身行为更明确。

errorMessage 把“等待错误元素 + 提取文字”封装起来。isDisplayed 同时检查 URL 没有 inventory 且有可见登录按钮，这比只看某个路径更明确，但仍不等于完整验证登录页全部控件。

## 4.4 把动作、观察和判断分开

测试文件描述业务意图：非法登录应被拒绝；页面对象负责定位与操作；WaitUtils 负责通用同步。选择器改名时，理想情况下改页面对象；需求文案变更时，应审查测试预期。这种分离降低维护成本，也让代码审查更容易发现“这条测试到底想证明什么”。

不要把它理解为页面对象绝不能包含任何条件。当前 awaitLoaded 会检查 URL 与元素可见性，这是页面可用性的保护；真正业务 expected 仍在测试层。ProductsPage.sortBy 甚至等待排序后的状态，这是一个具体取舍，第 7 章会讨论诊断上的后果。

**POM 缩写有歧义**：Page Object Model 是页面对象模式；pom.xml 的 POM 是 Project Object Model（Maven 项目模型）。看到缩写先看上下文，二者不是同一个东西。

## 4.5 失败路径同样要能画出来

如果用户名定位器错误，openBrowser 就可能失败，测试体不一定执行。报告可能出现配置失败与后续跳过。如果断言失败，监听器应在浏览器关闭前截图。如果 driver 创建成功但页面打开失败，driver 已经被存下，后置阶段仍能拿到它尝试关闭。如果 driver 创建本身失败，getDriver 可能是 null，因此清理和截图都做了保护。

这些分支告诉你：红色报告不一定是“登录需求坏了”。它可能是测试基础设施、同步、环境或业务失败。你需要沿执行阶段定位，不能看到失败就向产品开发报 bug。

## 4.6 quit 和 close，不只是方法名字不同

浏览器 WebDriver.close 关闭当前窗口；quit 结束整个 WebDriver 会话并关闭相关窗口。测试可能打开多个窗口，只 close 当前窗口不一定释放整个会话，所以基类使用 quit。数据库客户端也叫 close，但它是我们实现的另一个类型的方法，内部先 rollback 再关闭 Connection。相同方法名不能跨类型猜语义。

本项目使用的是立即失败的普通断言：一个 assert 抛出 AssertionError 后，测试体后面的语句通常不会继续执行；后置清理由框架安排。没有使用 SoftAssert 收集多个失败。因此金额测试最前面就失败时，后续金额关系本次可能根本没检查。报告通过可以支持整条成功路径都走过；报告失败则需要看停在哪里。

<details markdown="1"><summary>自测：为什么不让 CartTest 依赖 LoginTest 先成功运行？</summary>

那会把会话与执行顺序绑定。单独运行 CartTest 或分组运行时可能没有前置状态；并行时也可能属于不同浏览器。本项目让购物车测试自行调用 loginAsStandardUser，登录是它的准备步骤，而不是另一个测试留下的副作用。

</details>

官方背景：[Selenium Page Object 建议](https://www.selenium.dev/documentation/test_practices/encouraged/page_object_models/)。

**English checkpoint:** “The page objects encapsulate browser interactions. The test describes the expected behavior, and the base class manages the browser lifecycle.”
