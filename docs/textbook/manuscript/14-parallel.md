# 14 跨浏览器与并行：先隔离，再提速

> 本章目标：理解 headless、工厂方法、ThreadLocal、类并行及它们解决不了的问题。

## 14.1 相同测试，不同浏览器实现

{{source:src/main/java/driver/WebDriverFactory.java}}

工厂方法根据 browser 返回 WebDriver 接口。测试与页面对象面对接口，不必每个测试写 Chrome/Firefox 分支。ChromeOptions 与 FirefoxOptions 属于各自实现，headless 参数不同，工厂集中处理差异。

Headless 仍然是真浏览器渲染和执行 JavaScript，只是不显示普通交互窗口。它不是模拟 HTTP，也不是跳过 UI。固定 1280×900 让布局更可控，但不代表已经做响应式尺寸测试。

Chrome 关闭演示中可能干扰操作的密码保存/泄漏检测提示偏好；这不是通用安全测试配置。浏览器加载超时配置失败时，工厂会尝试退出已创建 driver，并保留清理异常。

Selenium Manager 管理驱动，缺失浏览器的下载取决于其支持与联网环境。项目本地 Firefox 首次由它下载成功。不要把一次成功下载描述成离线环境也必然能运行。

## 14.2 配置优先级不是所有模块都一样

{{source:src/main/java/config/ConfigManager.java}}

browser、baseUrl、username、password、waitSeconds 当前读取系统属性与默认值。headless 额外支持 HEADLESS 环境变量，且 -Dheadless 优先。DatabaseConfig 则给所有 DB_* 配置系统属性/环境变量/默认值三层。不能见到一个配置类就假设所有属性都有环境变量映射。

严格校验 true/false 能避免拼错 ture 却静默变成有窗口。对浏览器未知值直接抛异常，避免误以为 Firefox 已验证，实际仍跑 Chrome。

## 14.3 共享 driver 会怎样串号

教学反例：若所有类共用 static WebDriver，线程 A 创建浏览器 A 后，线程 B 把同一字段改成浏览器 B。A 接着读取字段，可能操作 B；A 的 teardown 又可能关闭 B。失败看起来像随机“窗口已关闭”“元素不存在”，根因是引用所有权。

ThreadLocal 提供当前线程对应的值。A 调 set(driverA)，B 调 set(driverB)，各自 get 取各自的值。它不会自动复制 WebDriver，不会替你 new 浏览器，也不会让传给其他线程的同一个 driver 变安全。

本项目 BaseTest 的 ThreadLocal 是实例字段，并非全局静态。按类并行通常已让各类实例自然分离；这里同时明确线程内 driver/page 引用，提供一致访问方式。不能因此认为整个项目已适配所有并行粒度。

## 14.4 为什么 driver 和 LoginPage 都隔离

LoginPage 内部持有 driver。若 driver 是线程局部而 LoginPage 仍共享可变字段，页面对象仍可能指向另一会话。相关对象必须有一致生命周期，而不是看到 ThreadLocal 这个词就停止审查。

quit 关闭外部浏览器会话；remove 清除线程内的引用。二者作用不同。测试运行器会复用线程，因此用完 remove，避免下次拿到旧对象或延长引用生命。

## 14.5 XML 选择类级并行

{{source:testng-web-parallel.xml}}

parallel=classes 意味着不同类可分配不同线程，同类方法按这一策略在同一线程中执行；thread-count=2 限制这个套件的工作线程数。这里没有启用 DataProvider 并行，也没有 API/DB 类。

一条类中的测试仍每次创建浏览器，不是“一类一个浏览器”。因此最忙时会有两个测试会话，而整个运行会反复创建与销毁。并行测试体是否时间重叠还取决于 setup 耗时与调度；两个线程不代表每个毫秒都有两个有效业务动作。

## 14.6 三层并发要分清

GitHub matrix 产生多个独立 job；每个 Web job 里 TestNG 采用两线程类并行；浏览器内部还有自己的运行机制。它们不是同一个线程池。Chrome job 与 Firefox job 在不同 runner 中，没必要共享同一份 Java ThreadLocal。

API 基类有 token/owned/trace 可变字段，DB 基类有 db 字段。当前不并行它们，所以没有宣称跨全部模块方法级安全。将来优化要先证明数据、连接、报告上下文和清理隔离，再增加线程。

速度提升要测量，不凭线程数宣称“两倍快”。网络延迟、浏览器启动、CPU 竞争与外部服务限流都可能改变结果。当前只记录通过与并行发生的证据，没有虚构百分比。

<details><summary>自测：使用 ThreadLocal 后，把同一个 WebDriver 显式传给另一个线程操作，安全吗？</summary>

ThreadLocal 不会阻止你传递引用，也不为 WebDriver 加锁。它只隔离通过 ThreadLocal.get/set 保存的线程值。仍须遵守每个会话由对应执行线程管理的设计。

</details>

官方查阅：[Java ThreadLocal](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ThreadLocal.html)。

**English checkpoint:** “I enable class-level parallelism only for Web tests. Thread-local browser and page references support isolation, while API and database tests remain serial.”
