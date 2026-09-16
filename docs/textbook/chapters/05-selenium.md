# 05 Selenium 与等待：浏览器不是同步函数

> 本章目标：理解 DOM、元素定位、显式等待与 stale element。能够分析 TimeoutException，而不只会加等待秒数。

## 5.1 WebDriver 到底控制什么

WebDriver 接口提供浏览器会话操作。ChromeDriver、FirefoxDriver 是实现。调用 Java 的 click 不等于直接调用电商后端的 Java 方法：命令需要交给浏览器自动化端执行，再触发页面事件和可能的网络请求。测试程序、浏览器页面和远端服务运行在不同时间线上。

DOM（Document Object Model）是浏览器对页面结构的对象表示。一个按钮可能存在于 DOM，但隐藏；可能可见，但 disabled；也可能可见且 enabled，却被遮罩挡住。不同条件对应不同可操作程度。

`By.id("login-button")` 根据 id 查找；`By.cssSelector("[data-test='error']")` 根据属性选择；`.cart_item .inventory_item_name` 表示 cart_item 内的后代节点，不要求直接子节点。这些 CSS 选择器是 Selenium 接收的查询语言，不是 Java 字段访问。

data-test 属性通常比页面排版层级更适合作为测试定位约定，但本项目也使用 class 和 id。没有“永远稳定的选择器”，只有与页面契约相对稳定的选择器。

## 5.2 findElement 和 findElements 的差别

findElement 需要单个匹配对象，找不到通常抛出 NoSuchElementException。findElements 返回列表，无匹配时可以得到空列表。所以检测可选 badge 用列表更自然：没有 badge 是合法状态，不需要把它当异常处理。

CartPage 的 itemCount 直接取列表长度，没有额外等待。它依赖之前的页面加载/动作等待已让页面达到可观察状态。若未来页面变为异步延迟更新，现有等待条件可能不足，需要围绕最终状态调整，而不是认为所有 getter 天然可靠。

## 5.3 显式等待的返回值


<p class="source-ref">原代码 · <a href="../source.html#s-23710c75a6af-L1" target="_blank" rel="noopener">src/main/java/utils/WaitUtils.java · L1–L33</a></p>

```java
package utils;

import config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Small condition helpers; page-specific conditions stay in their page objects. */
public final class WaitUtils {
    private final WebDriverWait wait;

    public WaitUtils(WebDriver driver) {
        wait = new WebDriverWait(driver, ConfigManager.waitTimeout());
    }

    public WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement clickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void urlContains(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    public void absent(By locator) {
        wait.until(ExpectedConditions.numberOfElementsToBe(locator, 0));
    }
}
```


以 visible 为例：ExpectedConditions.visibilityOfElementLocated(locator) 创建“寻找可见元素”的条件对象；until 接收这个条件，在超时时间内重复求值；条件成功时返回 WebElement；因此 visible 的返回类型也是 WebElement，调用者可以接着 getText 或 sendKeys。

它不是固定睡满 10 秒。条件很快满足就返回；一直不满足才超时。clickable 检查可见与启用，但不承诺没有遮罩或业务已准备好，点击仍可能因页面状态变化失败。

absent 使用匹配数量为 0，这与“隐藏但仍存在”不同。urlContains 只检查 URL 包含片段，也不是整个页面加载完成的证明，所以页面加载方法通常还等一个元素。

## 5.4 用时间线理解 race condition

```text
测试线程：click Continue ── 立即读错误文字？ ───────────────
浏览器：  接收事件 ── 校验输入 ── 更新 DOM ── 显示错误文字
                                      ↑
                         wait.visible(ERROR) 等待这个可观察条件
```

如果测试读得太早，没有文字不代表校验永远不会出现。反过来，错误文案永远不出现，等待也必须最终失败。可靠等待既允许合理延迟，又保留失败能力。

Thread.sleep 固定等待会在快速机器上浪费时间，在慢机器上仍不够。轮询业务条件更有意义。但不能把所有断言都塞进无限等待；等待要有清晰、有限的目标。项目的显式等待默认 10 秒、页面加载 30 秒，这些是不同阶段的预算，不是总测试时长上限。

## 5.5 什么时候出现 stale element

WebElement 是浏览器中某个 DOM 节点的引用，不是一张永远更新的截图。页面重建节点后，旧引用可能失效，产生 StaleElementReferenceException。重新定位通常比继续使用旧对象更合理。

ProductsPage.sortBy 的等待明确忽略 stale，并在轮询里重新获取排序控件和列表。这里是在短暂页面更新期间重试“读取状态”，不是忽略业务断言，也不是整条测试失败后再跑一次。超时仍会失败。

不要把 ignoring(Exception.class) 当稳定性方案。它可能把错误的定位器、程序错误甚至完全不相关的失败隐藏成漫长超时。忽略范围应该对应你确知可以暂时发生的状态。

## 5.6 等待和断言各自提供什么

wait.visible(ERROR) 回答“在规定时间内出现了可见错误区域吗”；assertEquals(actual, expected) 回答“错误区域解释对了吗”。只等待可见会放过错误文案；只立即读取文案则可能把时序问题误判为业务问题。两者结合才完整。

等待出现排序结果已经带有行为判断色彩。这样做可以减少早读，但如果排序算法错误，失败会是 TimeoutException，而不是后面更直观的 assertEquals 差异。当前实现就是这种取舍；面试时可以坦诚说明，不必声称每个设计都最佳。

<details markdown="1"><summary>自测：按钮已经 clickable，为什么 click 仍可能失败？</summary>

条件检测和点击不是原子操作，两者之间页面可能变化；可见且 enabled 也不表示没有遮挡。应根据异常、截图和页面状态选择正确同步条件，不能把 clickable 理解成未来点击绝对成功的保证。

</details>

官方补充：[Selenium 等待策略](https://www.selenium.dev/documentation/webdriver/waits/)。

**English checkpoint:** “I wait for observable conditions rather than adding fixed delays. A timeout tells me which condition was not met, but I still investigate whether the cause is the product, the locator, or the environment.”
