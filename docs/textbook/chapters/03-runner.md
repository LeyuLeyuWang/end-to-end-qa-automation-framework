# 03 Maven 与 TestNG：没有 main，代码为什么会运行

> 本章目标：理解构建、测试发现、注解、数据驱动和分组。遇到链式调用或注解时，先判断是谁提供的。

## 3.1 Maven 管构建，TestNG 管测试

在根目录执行 `./mvnw.cmd test`，不是直接运行一个叫 test 的 Java 方法。mvnw.cmd 是 Windows Maven Wrapper 入口，让项目使用约定 Maven 版本。Maven 读取 pom.xml，解析依赖，执行构建阶段，再由 Surefire 插件调用 TestNG。

```text
mvnw.cmd → Maven → pom.xml → compile / test-compile → Surefire → TestNG
                                                                  ↓
                                               suite / groups / @Test → Java 方法
```

`src/main/java` 存可复用的框架代码，`src/test/java` 存测试、基类、测试数据与监听器。本仓库的 main 不是电商业务后端，而是测试支持层。普通应用项目里 main 常放产品代码，这里不要机械套用。

Dependency 是编译/运行要用的库，例如 selenium-java；Plugin 是 Maven 构建时执行某项工作的组件，例如 maven-surefire-plugin。TestNG 与 Allure adapter 的 scope=test 表示它们属于测试编译/运行所需；Selenium 和 REST Assured 被 main 下的客户端使用，所以当前没有放在 test scope。


<p class="source-ref">原代码 · <a href="../source.html#s-9c5fb3d1b7e3-L1" target="_blank" rel="noopener">pom.xml · L1–L96</a></p>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.portfolio.qa</groupId>
    <artifactId>ecommerce-qa-automation</artifactId>
    <version>0.5.0-SNAPSHOT</version>
    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <selenium.version>4.49.0</selenium.version>
        <testng.version>7.12.0</testng.version>
        <rest-assured.version>6.0.1</rest-assured.version>
        <jackson.version>2.22.2</jackson.version>
        <postgresql.version>42.7.13</postgresql.version>
        <allure.version>2.35.5</allure.version>
        <allure.maven.version>2.18.0</allure.maven.version>
        <allure.commandline.version>2.46.1</allure.commandline.version>
        <suite.file>testng.xml</suite.file>
        <allure.results.directory>${project.build.directory}/allure-results</allure.results.directory>
        <surefire.reportsDirectory>${project.build.directory}/surefire-reports</surefire.reportsDirectory>
    </properties>
    <dependencies>
        <dependency>
            <groupId>io.qameta.allure</groupId>
            <artifactId>allure-testng</artifactId>
            <version>${allure.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgresql.version}</version>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>${rest-assured.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>${testng.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>io.qameta.allure</groupId>
                <artifactId>allure-maven</artifactId>
                <version>${allure.maven.version}</version>
                <configuration>
                    <reportVersion>${allure.commandline.version}</reportVersion>
                    <resultsDirectory>${allure.results.directory}</resultsDirectory>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.14.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.4</version>
                <configuration>
                    <reportsDirectory>${surefire.reportsDirectory}</reportsDirectory>
                    <systemPropertyVariables>
                        <allure.results.directory>${allure.results.directory}</allure.results.directory>
                    </systemPropertyVariables>
                    <suiteXmlFiles>
                        <suiteXmlFile>${suite.file}</suiteXmlFile>
                    </suiteXmlFiles>
                </configuration>
            </plugin>
        </plugins>
    </build>
    <profiles>
        <profile>
            <id>parallel-web</id>
            <properties><suite.file>testng-web-parallel.xml</suite.file></properties>
        </profile>
    </profiles>
</project>
```


`maven.compiler.release=17` 指定编译目标的 Java 版本/API 基线。本地验收使用 JDK 21，并不意味着代码必须写 Java 21 特性。`${...}` 在 pom 中是 Maven 属性引用，不是 Java 字符串模板。把版本集中在 properties 便于审查与升级，但固定版本不等于已经测试未来所有依赖组合。

`clean test` 中 clean 清理 Maven 输出目录，随后 test 运行默认构建生命周期到测试阶段；它不是重置数据库或删除所有截图。`allure:report` 是插件目标，读取结果生成 HTML，本身不会重新执行整个业务套件。

## 3.2 XML 中的 test 与 Java 的 @Test 不是同一层


<p class="source-ref">原代码 · <a href="../source.html#s-a871f1bb183b-L1" target="_blank" rel="noopener">testng.xml · L1–L22</a></p>

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="QA Automation V5">
    <test name="Web regression">
        <classes>
            <class name="tests.LoginTest"/>
            <class name="tests.ProductTest"/>
            <class name="tests.CartTest"/>
            <class name="tests.CheckoutTest"/>
        </classes>
    </test>
    <test name="API regression">
        <classes>
            <class name="api.AuthApiTest"/>
            <class name="api.BookingApiTest"/>
        </classes>
    </test>
    <test name="Database regression">
        <classes>
            <class name="database.DatabaseValidationTest"/>
        </classes>
    </test>
</suite>
```


suite 是套件；XML `<test>` 是一组类的逻辑容器；`@Test` 标记 Java 测试方法。默认 XML 明确列出四个 Web 类、两个 API 类和一个 DB 类。diagnostics 探针不在这里。

名称不是运行优先级。不要依赖源码上下排列来设计业务依赖。此项目的目标是每次测试自行准备条件；即便调度顺序改变也应成立。

## 3.3 注解是元数据，框架读取后才有行为

`@Test` 不是 Java 的“自动执行关键字”，它是 TestNG 提供的注解类型。Java 保存这类元数据，框架可以在运行时检查哪些方法被标记，再安排调用。这体现了控制反转：不是你在 main 里逐个调用，而是框架调用你编写的方法。

同样，`@BeforeMethod` 和 `@AfterMethod` 让 TestNG 安排前置/后置逻辑。基类中的这些方法也会参与子类测试生命周期。`alwaysRun=true` 让相关配置方法在分组选取或失败场景下尽可能执行；它不是操作系统崩溃、进程被杀时的清理保证。

`assertEquals` 来自 `org.testng.Assert`，不是 Java 的 `assert` 语句。Java assert 受 JVM 断言开关影响；TestNG 的方法调用直接执行。这里参数顺序是 actual、expected、可选 message。读别的框架时不要盲目沿用顺序。

## 3.4 DataProvider 把逻辑与输入分开


<p class="source-ref">原代码 · <a href="../source.html#s-5fba3cfe3b35-L1" target="_blank" rel="noopener">src/test/java/data/TestDataProvider.java · L1–L43</a></p>

```java
package data;

import config.ConfigManager;
import org.testng.annotations.DataProvider;
import pages.ProductsPage.Sort;

public final class TestDataProvider {
    private TestDataProvider() { }

    @DataProvider(name = "rejectedLogins")
    public static Object[][] rejectedLogins() {
        return new Object[][] {
            {"invalid password", ConfigManager.username(), "wrong_password",
                "Epic sadface: Username and password do not match any user in this service"},
            {"locked user", "locked_out_user", ConfigManager.password(),
                "Epic sadface: Sorry, this user has been locked out."},
            {"empty username", "", ConfigManager.password(), "Epic sadface: Username is required"},
            {"empty password", ConfigManager.username(), "", "Epic sadface: Password is required"},
            {"both empty", "", "", "Epic sadface: Username is required"}
        };
    }

    @DataProvider(name = "nameSorts")
    public static Object[][] nameSorts() {
        return new Object[][] {{Sort.NAME_ASC}, {Sort.NAME_DESC}};
    }

    @DataProvider(name = "priceSorts")
    public static Object[][] priceSorts() {
        return new Object[][] {{Sort.PRICE_ASC}, {Sort.PRICE_DESC}};
    }

    @DataProvider(name = "missingCheckoutFields")
    public static Object[][] missingCheckoutFields() {
        return new Object[][] {
            {"first name", "", "Customer", "90210", "Error: First Name is required"},
            {"last name", "Demo", "", "90210", "Error: Last Name is required"},
            {"postal code", "Demo", "Customer", "", "Error: Postal Code is required"},
            {"all fields", "", "", "", "Error: First Name is required"}
        };
    }
}

```


`Object[][]` 外层是一批调用，内层是一条调用的参数。rejectedLogins 每行依次对应 scenario、username、password、expected。TestNG 调用测试方法时负责把它们传进去。参数数量或类型不匹配会造成框架执行问题，不是产品缺陷。

为什么 scenario 是第一列？它让“空用户名”与“锁定用户”的失败信息可读。为什么不把五个场景写五份方法？步骤相同、输入与预期不同，数据驱动减少重复。反过来，完全不同业务过程不必硬塞进一个巨大 DataProvider。

同一个测试方法被调用五次，每次也应有独立前置/后置处理。这就是 2 个 LoginTest 方法产生 6 次执行的原因。当前 DataProvider 没有启用 parallel=true；不要看到二维数组就以为它自动并行。

## 3.5 groups 是标签，profile 是配置选择

`groups={"web","smoke","regression"}` 给同一个测试三个标签。`-Dgroups=web` 是通过 Maven/Surefire 传递的测试过滤配置。`-Pparallel-web` 则激活 Maven profile，将 suite.file 改为另一个 XML。这是两种不同层级的选择。

教学命令，在项目根目录执行：

```powershell
.\mvnw.cmd '-Dgroups=web' test
.\mvnw.cmd '-Dgroups=api' test
.\mvnw.cmd '-Dgroups=database' test
.\mvnw.cmd -Pparallel-web '-Dgroups=smoke' '-Dheadless=true' test
```

最后一条只运行并行 Web 套件中的 smoke，而不是全项目 9 个。PowerShell 中将 `-D...` 参数整体加引号，可以避免复杂属性名被 shell 错误解析。引号是 shell 的参数边界，不属于 Java 属性值。

<details markdown="1"><summary>自测：把数据库密码写进 pom.xml，然后运行 -Dgroups=web，会不会因此创建数据库连接？</summary>

单纯存在配置不会自动创建连接。连接由被调度测试的生命周期代码触发。Web 继承 BaseTest，不继承 BaseDatabaseTest。但真实密码不应硬编码在源码，是否使用和是否适合存储是两个问题。

</details>

官方查阅入口：[TestNG 注解、分组与数据驱动](https://testng.org/)。本章执行路径以仓库 pom 和 XML 为依据。

**English checkpoint:** “Maven builds the project, Surefire invokes TestNG, and TestNG schedules the annotated methods and their setup and teardown hooks.”
