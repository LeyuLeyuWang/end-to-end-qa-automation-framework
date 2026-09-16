# 01 学习地图：你究竟在学习什么

> 本章目标：分清被测系统、测试代码、测试框架和开发工具；知道这份教材的阅读路线。Prerequisite：Java 基础语法。

## 1.1 从会写 Java 到会设计测试

你已经知道变量、类、方法、循环，但读自动化代码仍可能觉得陌生。原因通常不是 Java 太难，而是执行模型变了。普通练习里，你写 main，再主动调用方法；在这里，你给方法加注解，由 TestNG 在合适的时间调用。你控制浏览器、发送请求、打开数据库连接，再根据证据判断某个行为是否正确。

学习的目标不是记住 `click()`、`get()` 和 `assertEquals()`，而是能解释一条完整的因果链：**业务风险是什么 → 如何准备条件 → 做什么操作 → 观察什么结果 → 为什么这个结果足以支持结论 → 测完怎样恢复环境**。整本书会反复沿着这条链读源码。

例如“删除购物车里的背包”不能只看删除按钮是否可以点击。用户真正关心的是背包消失、其他商品还在、购物车数量正确。按钮点击是动作，剩余商品列表才是判断业务是否正确的重要证据。

## 1.2 四种东西先分清

| 名称 | 英文 | 在项目中的例子 | 它回答什么问题 |
|---|---|---|---|
| 被测系统 | System Under Test / SUT | SauceDemo、Restful Booker、PostgreSQL fixture | 我们在检查谁？ |
| 测试用例 | Test case | addedProductAppearsInCart | 检查什么行为？ |
| 测试框架/库 | Framework / library | TestNG、Selenium、REST Assured | 怎样组织、执行和判断？ |
| 测试基础设施 | Test infrastructure | Maven、Docker、GitHub Actions、Allure | 在哪里运行，怎样准备和查看结果？ |

TestNG 是测试运行与组织框架；Selenium 是浏览器自动化工具；REST Assured 是 HTTP 测试库。Allure 是报告工具，不负责认定业务预期。Docker 运行 PostgreSQL，也不会帮你决定哪条 SQL 算正确。

“Framework” 在面试里还可能指你搭建的整套结构：基类、客户端、页面对象、配置、数据、监听器、报告和工作流。它不是某个依赖包，而是多个工具围绕测试目标的组合。

## 1.3 当前项目的事实边界

项目包含三个独立目标。Web 模块通过浏览器操作 SauceDemo；API 模块操作公开 Restful Booker 服务；数据库模块连接本地 Docker PostgreSQL。**它们没有共享订单，没有共享用户，也不是同一个应用的三层。** 页面完成结账，不会在本项目 PostgreSQL 里产生订单；创建 booking 也不会出现在 SauceDemo 购物车。

因此你可以说本项目展示了 Web、API、数据库三个方向的自动化能力。不能说它已经实现“前端下单 → 自家 API → 后端数据库核对”的贯通测试。仓库名称中的 End-to-End 不能替代实际系统连接。Web 内部的结账流程可以作为用户流程测试来描述，但仍没有验证真实支付、发货或邮件。

| 业务类 | @Test 方法数 | 参数展开后的执行数 |
|---|---:|---:|
| LoginTest | 2 | 6 |
| ProductTest | 3 | 5 |
| CartTest | 6 | 6 |
| CheckoutTest | 4 | 7 |
| AuthApiTest | 2 | 2 |
| BookingApiTest | 10 | 12 |
| DatabaseValidationTest | 8 | 8 |
| 合计 | 35 | 46 |

一个带 5 行数据的测试方法会调用 5 次。Web 在 Chrome 和 Firefox 各跑 24 次，代表同一批场景在两个浏览器上验证，不代表设计了 48 个不同业务场景。故意失败的 diagnostics 探针不计入 46。V3 Appium 按开发决定跳过，没有移动端测试。

## 1.4 版本演进的真正含义

V0 让测试跑起来，学习浏览器操作和断言。V1 抽出页面对象、生命周期、等待和数据，让业务意图更清楚。V2 加入 API、认证、数据所有权和清理。V4 加入独立数据库 fixture、SQL 和事务回滚。V5 加入浏览器切换、无头、Web 类并行、Allure 和 CI 配置。

这不是“每升一级就多一种工具”的收集任务。每次升级都应解决实际问题：页面选择器重复怎么维护？接口数据会不会污染公共服务？SQL 测试失败会不会留下数据？并行时浏览器会不会串号？失败时有没有证据？你能把这些问题和代码对应起来，才算理解版本价值。

## 1.5 怎样读本书

建议先读 1—4 章建立测试和运行模型；5—8 章读 Web；9—11 章读 API；12—13 章读数据库与 Docker；14—16 章读并行、报告和 CI；最后用 17—18 章调试、练习和面试复述。章节不会重新教授 for 循环，却会拆开你可能不熟悉的 Lambda、泛型、record、资源管理和框架回调。

代码区分为“原代码”和“教学示例”。原代码由生成器从本仓库提取，带文件与行号；教学示例用于说明一个概念，不表示已加入业务套件。代码是当前 V5 的快照，后续修改源码后可以重新生成教材。

阅读时做三种笔记即可：我现在相信的业务规则、我仍不确定的框架行为、我想尝试的反例。不要只抄定义。“假如 API 返回 403 但数据仍被改了，这条测试会发现吗？”比背诵 HTTP 状态码更有价值。

<details><summary>自测：为什么 46 个测试全部通过，不代表整个电商系统没有问题？</summary>

因为这里是有限场景、有限输入、有限环境下的证据，且三个模块测试三个独立目标。未覆盖的功能、数据组合、浏览器、真实业务集成仍可能出问题。通过说明观测到的行为满足这些断言，不能推导出无缺陷或全面覆盖。

</details>

**English checkpoint:** “This project demonstrates Web, API, and database testing against three independent targets. I distinguish unique test scenarios from repeated executions across browsers.”
