# 18 综合练习与面试：怎样知道自己真的懂了

> 本章目标：从“读得懂”走到“能预测、能修改、能解释边界”。先作答，再展开参考思路。

## 18.1 用能力检验学习，不用阅读时长

读完一遍只是熟悉。真正理解至少包含四件事：不看解释能画执行链；给定错误能预测哪条断言发现；能增加一个小场景并正确准备/清理；能说明测试通过不能证明什么。不要求背 API 全名，允许查文档，但需要知道查什么。

建议每次完成一个模块就做下面的练习。练习是待你实现的任务，不代表仓库已经新增这些测试。正常业务场景数量仍为 46。

## 18.2 Web 练习：保留状态与排序反例

**练习 1：交换商品角色。** 现有 removingOneProductPreservesTheOther 删除背包保留车灯。请设计删除车灯保留背包的版本，先写 AAA 与 expected，再决定是否需要 DataProvider。

<details><summary>参考思路</summary>

准备独立登录会话，添加两个指定商品；删除车灯；断言只剩背包、数量为 1、继续购物后 badge=1。若把两种方向合并进 DataProvider，参数可包含 removeProduct/remainingProduct。保持每次独立准备，不复用上一行购物车。

</details>

**练习 2：增强价格排序。** 怎样发现“名称和价格集合都对，但价格绑错商品”？

<details><summary>参考思路</summary>

在操作前按每个商品卡片读取 name-price 成对值，用 record 表示；排序后再次读每个卡片，比较成员映射保持，并检查顺序符合价格方向。不要分别从两个不对应的列表胡乱 zip，先确保同一 DOM 商品容器内提取。新增前先读现有 ProductsPage 的结构。

</details>

**练习 3：删除等待。** 把 LoginPage.errorMessage 的等待换成立即 findElement，可能怎样失败？

<details><summary>参考思路</summary>

校验 DOM 未更新时可能找不到元素，产生偶发 NoSuchElementException；不是必然每次失败。需要等错误区域出现，再断言文本。实验应单独保留，不要为了展示故障把主代码永久改坏。

</details>

## 18.3 API 练习：拒绝必须不产生副作用

**练习 4：无效 Token 的 DELETE。** 当前 invalidToken 仅测试 PATCH，如何扩展 DELETE？

<details><summary>参考思路</summary>

先创建 owned booking；用 invalid-token DELETE，预期 403；随后 GET 完整对象等于 original；最后正常 token 清理。不能用他人的 ID，不要把响应 403 作为唯一断言。考虑参数化不同方法但保持行为清晰。

</details>

**练习 5：修改 firstname。** 为什么当前清理可能拒绝删除？怎样修改设计才合理？

<details><summary>参考思路</summary>

owned 存的是原 firstname，清理会比较服务器当前值。若用例合法改了它，检查不匹配。可以在已验证更新后有控制地更新登记的预期标记，或采用不随业务修改的所有权标记/独立测试环境；不能直接移除所有所有权检查。还要考虑更新请求失败后实际状态不确定的情况。

</details>

**练习 6：创建响应回显正确、GET 错误。** 哪条用例发现？

<details><summary>参考思路</summary>

createdBookingCanBeFetched 会发现；仅比较创建 body 的用例未必发现。PUT/PATCH 测试同样通过随后的 GET 增强验证。不要把一次响应回显当成持久化结论。

</details>

## 18.4 数据库练习：错误类型与事务范围

**练习 7：负数金额约束。** schema 已定义非负金额，如何补一个具体用例？

<details><summary>参考思路</summary>

用合法 seed user 和其他合法字段插入负 total_amount，捕获 SQLException 并核对 PostgreSQL CHECK 违反对应 SQLSTATE（实现前查官方错误码），让基类回滚。不能只捕获任意异常，因为连接失败也会假通过。此练习当前未加入 8 个场景。

</details>

**练习 8：回滚之后 ID 不连续。** 是不是隔离失效？

<details><summary>参考思路</summary>

不是。序列消耗与普通行回滚不同。用查询确认行不存在，不要求 ID 连续。判断隔离要看数据状态，不看计数器是否倒退。

</details>

**练习 9：SQL 拼错却通过“预期异常”测试。** 怎样避免？

<details><summary>参考思路</summary>

在 expectThrows 后核对具体 SQLSTATE 或更明确约束信息。现有外键测试检查 23503，区分外键与语法/连接错误。输入要让其他前提合法，集中触发目标约束。

</details>

## 18.5 框架练习：预测调度与报告

**练习 10：把 parallel-web 设成 methods，并加入 API 类，可以直接宣称线程安全吗？**

<details><summary>参考思路</summary>

不能。ThreadLocal 仅覆盖 Web driver/page；API token、trace、owned 与 DB connection 等状态仍需审查。并行模式变化也可能改变调用与清理交错。需要先隔离共享资源，再做有针对性的并发验收。

</details>

**练习 11：两次 DataProvider 失败只有一张截图，优先看什么？**

<details><summary>参考思路</summary>

文件命名是否覆盖、附件是否分别关联结果、截图是否在 teardown 前、会话是否已经死亡。当前文件名有时间戳与 UUID，探针预期两条失败各一个附件。文件存在和报告关联正确要分别检查。

</details>

**练习 12：clean test 后截图文件夹仍有旧 PNG，是否异常？**

<details><summary>参考思路</summary>

未必。Maven clean 清理 target，而当前截图目录是 test-output/screenshots。应按结果关联或时间识别本轮，不按目录总数判断失败数。Allure 原始结果通常在 target，生命周期与截图目录不同。

</details>

## 18.6 用 90 秒介绍项目

下面是基于当前代码事实的英文表达。先理解意思，再用自己的话说，不必背成演讲稿。

> I built a Java QA automation project with Selenium, TestNG, REST Assured, and JDBC. It contains 24 Web scenarios, 14 API scenarios, and 8 database scenarios. The three modules target independent systems: SauceDemo, Restful Booker, and a local PostgreSQL fixture.
>
> For Web tests, I use page objects, explicit waits, and a fresh browser session for each test. The API tests create uniquely identified bookings and clean up only their own resources. Database tests use parameterized SQL and roll back each test transaction.
>
> I added Chrome and Firefox support, headless execution, class-level Web parallelism, and Allure failure screenshots. The GitHub Actions workflow is configured and statically checked; hosted execution is still pending repository publication.

中文逻辑是：先范围，再设计理由，最后验证与限制。不要一口气罗列十个工具却解释不了最简单的登录测试。

## 18.7 常见追问的回答骨架

“为什么使用 POM？”——定位与页面操作集中管理，测试保留业务意图；举 LoginPage 与 LoginTest。不是“因为行业都这样”。

“怎样避免 flaky？”——具体谈显式条件、新会话、独立数据与证据；承认公共服务依赖。不是承诺永不波动。

“怎么测 API 更新？”——对响应做契约和值检查，再 GET；PATCH 还核对未修改字段。

“为什么用 rollback？”——我们控制该 JDBC 连接，可撤销本测试未提交写入；API 外部服务写入不在范围，所以另做清理。

“你实现了哪些安全测试？”——只有有限认证拒绝与参数绑定演示，没有完整权限、安全扫描或渗透覆盖。

“CI 跑通了吗？”——按实际状态回答；配置和静态校验不等于云端成功。

## 18.8 毕业任务：一个可审查的小改动

任选上面一个扩展场景，提交给自己一份说明：风险、前置条件、操作、预期、隔离、失败证据、未覆盖内容。再写代码，单独运行，必要时运行受影响分组。最后解释为什么这些检查足够支持这次改动，而没有盲目全量重复。

如果你能不靠逐行讲解完成这个过程，并回答“我的测试可能漏掉什么”，你已经从读代码进入测试工程实践。工具名会变化，这种思考方式会继续有用。
