# 15 失败诊断与 Allure：报告如何形成

> 本章目标：理解监听器回调、截图时机、报告原始数据和故意失败探针。

## 15.1 失败时最有价值的是现场

测试失败之后，只有一句 expected true but found false 很难定位。截图可以显示当时页面，但浏览器退出后再截图通常已失去现场。TestListener 因此使用方法调用后的回调，在 @AfterMethod 退出之前捕获。

{{source:src/test/java/listeners/TestListener.java}}

IInvokedMethodListener 来自 TestNG，TestListener 是我们实现它的类。implements 说明承诺实现接口；@Override 让编译器检查方法是否真覆盖。框架调用 afterInvocation 时提供方法描述与 ITestResult，不是我们主动在每条测试后调用。

回调也可能涉及配置方法，所以代码用 method.isTestMethod 限制 Web 测试标签逻辑。截图分支则检查失败状态、是否已有 screenshot 属性、对象是否是 BaseTest、driver 是否存在。这个短路条件让 API 和数据库不误走浏览器截图。

`instanceof BaseTest test` 是类型模式匹配，检查后顺便得到对应类型变量；不是新建 BaseTest。ITestResult 的 attribute 是这次结果携带的自定义信息，用于避免重复保存。

## 15.2 截图文件为什么带时间戳和 UUID

{{source:src/main/java/utils/ScreenshotUtils.java}}

DataProvider 同一个方法会执行多次，并行时也可能同时失败。只用方法名命名会覆盖。时间戳加 UUID 降低冲突，safeName 去掉不适合文件名的字符。输出目录按需创建，截图 bytes 写入 PNG 文件并返回绝对路径。

TakesScreenshot 是 Selenium 能力接口，强制转换表示当前 driver 支持截图。当前 Chrome/Firefox 支持；若换成不支持的实现或会话已经崩溃，截图仍可能失败，监听器只记录截图错误，不替换原始测试失败。

## 15.3 文件与 Allure 附件是两步

文件写到 test-output/screenshots 后，还需 Allure.addAttachment 把内容关联到当前测试报告。只是保存 PNG 不会自动出现在 Allure。Files.newInputStream 使用 try-with-resources，附加完成后关闭输入流。

Allure TestNG adapter 记录测试状态、参数与异常等；我们的 Listener 补充 browser/headless 和失败附件。无需给每条测试手工调用“记录通过”。本项目通过 adapter 的集成方式生成结果，没有自行重造运行器。

## 15.4 密码为什么不能只视觉隐藏

登录测试参数包含 password。当前按 arg2 或 password 参数名把 Allure 参数值替换成 [REDACTED]，修改的是结果值，不只是页面展示模式。之前验收发现参数列表顺序和 Java 方法顺序不同，因此不能按列表第 3 项盲改。

这段逻辑是针对当前 invalidLoginIsRejected 方法的窄范围处理。重命名或新增带秘密的参数时需要重新审查。Surefire 的报告参数没有同样的完整脱敏保证；auth API 日志另有过滤。不要把一个局部处理说成整套系统所有产物自动安全。

## 15.5 原始结果、HTML、Surefire 的区别

| 位置 | 用途 |
|---|---|
| target/allure-results | adapter 生成的 JSON 与附件，可用于重新生成报告 |
| target/site/allure-maven-plugin | Allure CLI 生成的 HTML 和静态资源 |
| target/surefire-reports | Maven 测试执行的 XML/文本等结果 |
| test-output/screenshots | 独立保存的失败 PNG |

先 test，后 allure:report。allure:serve 生成/提供本地可浏览报告，适合查看；直接双击 report 的 index.html 可能遇到浏览器本地资源限制。教材本身则做成自包含 HTML，可以直接打开，两者实现不同。

多次运行如果往同一结果目录追加，报告可能混入旧结果。clean test 或独立结果目录可以明确一次运行边界。clean 不会自动删除 test-output 中的截图，不能看文件夹里有多少 PNG 就当本轮失败数。

## 15.6 用探针验证失败路径

{{source:src/test/java/diagnostics/ScreenshotProbe.java}}

failureCases 两行对应两次故意 fail，另一个方法正常通过。预期是两个失败结果各有附件、成功没有截图、退出码非零。看到 BUILD FAILURE 在这里是验收预期，不能为了绿灯把 fail 删掉。

```powershell
.\mvnw.cmd '-Dtest=ScreenshotProbe' '-Dheadless=true' '-Dallure.results.directory=target/probe-allure-results' '-Dsurefire.reportsDirectory=target/probe-reports' test
```

它是显式诊断，不在业务套件，也不满足默认 *Test 命名模式。API/DB 探针同理。这种设计让正常回归绿灯与基础设施失败路径验证分开。

<details><summary>自测：报告显示某个测试体通过，但 teardown 失败，可以宣称本轮完全成功吗？</summary>

不可以。后置失败可能意味着数据残留或资源未释放。查看 Maven 总体结果和 fixture/configuration 失败。仅按绿色测试体数量下结论会遗漏环境责任。

</details>

官方查阅：[Allure TestNG 集成](https://allurereport.org/docs/testng/)。

**English checkpoint:** “I capture screenshots before teardown and attach them to Allure. Diagnostic probes deliberately fail to verify that failure evidence and cleanup actually work.”
