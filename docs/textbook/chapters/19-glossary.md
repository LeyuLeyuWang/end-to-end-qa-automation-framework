# 19 术语速查与学习索引

> 遇到术语先查这里，再回到对应章节。短定义用于定位，具体边界以正文和源码为准。

| 术语 | 中文理解 | 对应章节 |
|---|---|---|
| SUT | 被测系统，测试真正观察的对象 | 1 |
| Test oracle | 判定正确与否的依据 | 2 |
| Assertion | 将实际结果与期望进行可失败的检查 | 2、3 |
| AAA | Arrange / Act / Assert，准备、动作、断言 | 2 |
| Fixture | 测试所需的受控环境或数据 | 11—13 |
| Isolation | 不依赖其他测试的可变状态，避免相互污染 | 2、11、12、14 |
| Positive / Negative test | 验证允许行为 / 正确拒绝行为 | 2、10 |
| Regression | 检查已有行为未被破坏 | 2、3 |
| Smoke | 少量关键路径的快速检查集合 | 2、3 |
| State transition | 操作引起的业务状态变化 | 7、8 |
| Invariant | 操作前后应保持的性质 | 2、7、10 |
| Characterization test | 描述当前已知行为，即使行为并不理想 | 10 |
| DataProvider | TestNG 提供多组测试参数的机制 | 3 |
| Setup / Teardown | 测试前准备 / 测试后清理 | 4 |
| Listener / Callback | 框架发生事件时调用我们提供的方法 | 3、15 |
| Page Object Model | 集中页面定位、动作和观察的组织方式 | 4 |
| DOM | 浏览器中的文档结构对象模型 | 5 |
| Locator | 定位元素的规则，例如 By.id | 5 |
| Explicit wait | 在有限时间内轮询具体条件 | 5 |
| Stale element | 原有 DOM 节点引用失效 | 5 |
| Fluent API | 通过返回对象实现连贯链式调用 | 4、9 |
| Serialization | Java 对象转为 JSON 等表示 | 9 |
| Deserialization | JSON 等表示转成 Java 对象 | 9 |
| Authentication | 验证身份凭据 | 9 |
| Authorization | 判断是否允许某项操作 | 9、10 |
| Idempotency | 重复操作对目标状态的预期效果不继续累加；当前没有完整幂等性测试 | 11 的边界讨论 |
| Ownership | 识别哪些测试数据由自己创建和负责清理 | 11 |
| Transaction | 一组数据库操作的事务边界 | 12 |
| Commit / Rollback | 提交修改 / 撤销该事务未提交修改 | 12 |
| PreparedStatement | SQL 结构与数据参数分离的 JDBC 命令 | 12 |
| ResultSet | 查询结果游标 | 12 |
| RowMapper | 把当前数据库行转换为类型化 Java 值 | 6、12 |
| SQLSTATE | 数据库错误分类代码 | 13 |
| Primary / Foreign key | 主键识别行；外键限制引用关系 | 13 |
| Image / Container / Volume | 镜像模板 / 运行实例 / 持久数据 | 13 |
| Headless | 不显示普通窗口的浏览器执行模式 | 14 |
| ThreadLocal | 按当前线程存取各自的值 | 14 |
| Race condition | 执行交错或时机改变结果 | 5、14 |
| Artifact | 保存供查看的运行产物 | 15、16 |
| CI | 自动触发构建与测试的持续集成 | 16 |
| Matrix | 将配置组合展开为多个 job | 16 |
| Flaky | 在相关条件相同下结果不稳定的测试表现 | 17 |

## 谁提供这个方法？

| 写法 | 提供者 | 怎样追踪 |
|---|---|---|
| System.getProperty / ThreadLocal / BigDecimal | Java 标准库 | 查 Java API |
| @Test / assertEquals / ITestResult | TestNG | 查 import 与 TestNG 文档 |
| WebDriver / By / WebDriverWait / Select | Selenium | 查 Selenium API |
| given / Response / jsonPath | REST Assured | 查类型与库文档 |
| Connection / PreparedStatement | JDBC 标准接口 | 查 java.sql；实现由 pgJDBC 提供 |
| Allure.addAttachment | Allure | 查 adapter/runtime API |
| loginAsStandardUser / createTracked / seedUser | 本项目 | 在仓库搜索定义 |
| loginPage().errorMessage / products.sortBy | 本项目 Page Object | 找页面类，再看内部框架调用 |

遇到不熟悉的一行，先回答“这是哪一层的 API”，再去阅读正确来源。框架代码通常只是普通 Java 对象协作，加上第三方约定的调用时机。

## 本教材对应的代码与验收时间

以生成时的 V5 源码为准，构建清单记录源文件 SHA-256，方便以后判断是否过时。V5 历史本地验收为 2026-09-14：完整 46 通过，Chrome/Firefox Web 并行各 24 通过，截图探针 2 预期失败 + 1 通过。本文编写不重新声称跑过业务套件；它使用已有交接记录并核对当前实现。

当前没实现移动端、负载测试、完整安全测试、真实支付或三个目标贯通的数据核验。云端工作流尚待 GitHub 实跑。这些边界同样属于教材内容，而不是需要隐藏的缺点。
