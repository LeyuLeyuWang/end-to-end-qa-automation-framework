# V5 开发交接

供 ChatGPT Chat 教学使用。本任务负责开发与验收，讲解可从以下改动顺序展开。

## 本版范围

保留 V4 的 24 Web + 14 API + 8 DB = 46 个业务场景（35 个业务 @Test 方法）。V3 Appium 按决定跳过；没有移动端实现或移动端覆盖声明。

1. ConfigManager：headless 系统属性 / HEADLESS 环境变量，默认 false，严格校验。
2. WebDriverFactory：Chrome / Firefox 分支，复用业务测试，设置无头参数和窗口尺寸。
3. BaseTest：ThreadLocal<WebDriver> 与 ThreadLocal<LoginPage>，每次测试独立浏览器，finally 清除线程引用。
4. LoginTest、ScreenshotProbe：通过 loginPage() 访问当前线程页面。
5. testng-web-parallel.xml + Maven parallel-web profile：只让 Web 类以 2 线程并行；默认完整套件串行。
6. pom.xml：0.5.0-SNAPSHOT，Allure TestNG 2.35.5、Maven 插件 2.18.0、CLI 2.46.1；无需 AspectJ。
7. TestListener：沿用关闭浏览器前截图的时机，将 PNG 附加到 Allure，添加 browser/headless 标签。登录 DataProvider 密码按参数名称脱敏，不依赖 Allure 参数列表顺序。Surefire 参数不保证脱敏，只用演示凭据。
8. .github/workflows/qa-tests.yml：push/PR/手动触发，Chrome、Firefox、API、数据库共 4 个 job；缓存 Maven，数据库健康等待，失败也生成/上传报告，最终保持失败状态。

## 本地验收（2026-09-14，Windows，Java 21）

- Chrome 无头完整回归：46/46 通过，0 失败/跳过。
- Chrome 无头 Web 类并行：24/24 通过。
- Firefox 无头 Web 类并行：24/24 通过；Selenium Manager 自动下载缺失浏览器。
- Allure 的 thread 标签确认两种浏览器均使用两个线程；Chrome 测试体时间区间确认不同类确实重叠执行。
- ScreenshotProbe：预期 2 失败 + 1 成功；两条失败结果各有独立 PNG，检查了 PNG 文件签名，成功无附件。
- 修正脱敏后，5 个 rejected login 结果的 arg2 均为 [REDACTED]。
- actionlint 1.7.12 静态校验工作流通过。
- 故意失败探针单独保存在 target/probe-*，不混入业务报告。

本地日志保存在忽略目录 .tools/v5-*.log。报告和截图也被忽略，不作为源码提交。

## 云端尚待验收

当前目录不是 Git 仓库，没有 GitHub 远端。不能把静态校验描述成 GitHub Actions 已成功运行。上传仓库后需要检查真实的四个 job、Allure HTML 和原始结果上传，包括失败运行时的产物。

数据库是独立本地 fixture，与两个公共测试站点不连接。公开站点、网络、浏览器下载变化都可能影响运行。本版没有自动重试，没有性能提升或覆盖率百分比声明。

## 教学建议

先看浏览器配置 → 工厂 → ThreadLocal 生命周期 → 并行 XML → 监听器和 Allure → CI。重点区分测试业务场景与运行矩阵、测试失败与构建失败、线程隔离与数据隔离、报告原始结果与 HTML、配置好 CI 与在云端实际验收。

运行命令详见根 README。V4 对照资料是 docs/V4_README.md 和 docs/v4-baseline.zip。


最终复验：修正报告脱敏后再次运行完整 46 场景，全部通过；allure:report 成功生成 HTML，widgets/summary.json 确认 passed=46、total=46、failed/broken/skipped=0。最终 5 个登录失败参数均已脱敏。没有残留 chromedriver/geckodriver 进程。

可在项目根目录执行 `.\mvnw.cmd allure:serve` 查看最终报告。
