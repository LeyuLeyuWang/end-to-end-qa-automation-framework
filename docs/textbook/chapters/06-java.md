# 06 Java 进阶速通：读懂框架代码的语法

> 本章目标：把陌生写法还原为你熟悉的对象和方法调用。本章例子来自项目，但不要求背下所有语法。

## 6.1 var 没有取消类型

`var products = loginAsStandardUser();` 中编译器根据返回类型推断 ProductsPage。它不是 JavaScript 的动态变量；随后不能给 products 赋一个字符串。看到 var 时，沿方法声明查返回类型，这是读此项目很实用的习惯。

静态导入 `import static org.testng.Assert.*;` 允许省略 Assert 前缀。所以 `assertTrue(...)` 不是当前测试类自己定义的方法，也不是语言内建关键字。普通 import 导入类型名，static import 导入静态成员。

## 6.2 Lambda 是可以交给别人的行为

数据库的 `row -> row.getLong("id")` 不是立即读取某个全局 row，而是描述“给我一行时怎样转换”。DatabaseClient 遍历 ResultSet 时才调用 mapper.map(rows)，此时 Lambda 执行。框架回调和数据转换都大量使用这种方式。

教学示例，下面两种表达相近：

```java
DatabaseClient.RowMapper<Long> mapper = row -> row.getLong("id");

DatabaseClient.RowMapper<Long> expanded = new DatabaseClient.RowMapper<Long>() {
    @Override
    public Long map(java.sql.ResultSet row) throws java.sql.SQLException {
        return row.getLong("id");
    }
};
```

@FunctionalInterface 表明接口只有一个抽象方法的约束，便于 Lambda 作为它的实现。Lambda 不等于新线程；调用它的人在哪个线程执行，它通常就在那个调用线程里执行。

## 6.3 Stream 是变换管道

`elements.stream().map(element -> element.getText()).toList()` 的过程是：获得元素流；把每个 WebElement 转成 String；收集为 List<String>。不是 map 修改了页面，而是从页面读取并建立 Java 集合。

`filter` 保留满足条件的元素；`findFirst` 返回 Optional；`orElseThrow` 处理没找到的情况。CartPage.quantity 就先按商品名筛选，再读取数量，如果商品不存在则明确抛异常。这样比无条件取第一个元素更能表达意图。

`BigDecimal::add` 是方法引用，可以把它理解为 `(left, right) -> left.add(right)`。`reduce(BigDecimal.ZERO, BigDecimal::add)` 从零开始累计价格。这里的 Stream 没有调用 parallelStream，不因为用了 Lambda 就并发。

## 6.4 泛型让工具返回业务需要的类型

DatabaseClient 的 `<T> List<T> query(...)` 意味着每次调用可以选择不同 T。读取 id 时是 Long，读取金额时是 BigDecimal，读取聚合结果时是 Totals。编译器通过 RowMapper 的返回值推断。

`Object... parameters` 是可变参数，调用处可以传一个或多个参数，方法里作为数组处理。它便利但不是完整 SQL 类型校验：把字符串传给数值列，可能直到数据库执行才报错。泛型 RowMapper 改善返回值类型，并未让 SQL 字符串变成编译期验证的查询。

ProductsPage 中 `<T extends Comparable<? super T>>` 可以先读成“支持比较的 T”。排序检查需要调用 compareTo；这个类型约束让 String 和 BigDecimal 等类型共用有序性检查。`? super T` 允许比较能力定义在 T 的父类型上，不必为了懂测试先背复杂通配符规则。

## 6.5 record 与 enum

Booking 是 record，编译器提供构造器、访问器、equals/hashCode 等。访问 firstname 使用 firstname()。当前字段是字符串、基本类型和 BookingDates record，适合当值对象使用。record 只保证组件引用不能重新赋值；若组件本身是可变集合，不能因此宣称深度不可变。

测试用 `assertEquals(actualBooking, expectedBooking)` 时，record 的值比较让多个字段一起参与，避免手写每个 getter 断言。若实际 JSON 解析失败，会在比较前失败；值相等不自动证明原始 JSON 的所有额外字段都被严格拒绝。

Product enum 将 DOM id 后缀和商品显示名绑定。Sort enum 将 NAME_ASC 与页面 select 的值 az 绑定。这是类型安全的命名，减少测试里散落的魔法字符串。enum 不是数据库里完整商品目录；当前 Product 只列了三个便于引用的商品。

## 6.6 BigDecimal 为什么值得单独注意

`new BigDecimal("29.99")` 从十进制字符串建立精确值；用二进制浮点数构造可能把表示误差带进来。金额断言通常避免 double 直接相等。

BigDecimal 的 equals 还比较 scale，例如 1.0 与 1.00 不相等；compareTo 为 0 表示数值相同。项目对某些固定 fixture 金额用 equals，对相加关系用 compareTo。并非一个永远正确、另一个永远错误，应根据你要验证“金额数值”还是“值与表示精度”选择。

## 6.7 try-with-resources 与 suppressed exceptions

`try (PreparedStatement ...; ResultSet ...)` 会在离开作用域时自动关闭资源，包含异常退出；关闭顺序与声明顺序相反。实现 AutoCloseable 的对象可以参与这种语法。DatabaseClient 自己也实现它，不过基类当前显式在后置方法里调用 close。

若原操作失败、清理也失败，只抛后者会掩盖起因。addSuppressed 把次要错误挂到主要异常上。它不同于 cause：cause 常表示包装异常的直接原因，suppressed 表示处理同一过程时另一个没有作为主异常抛出的错误。项目在数据库、浏览器工厂和 API 清理中保留这些信息。

<details markdown="1"><summary>自测：row → row.getLong("id") 是在哪个时刻执行？它会自动异步吗？</summary>

query 执行 SQL 后，在 while(rows.next()) 内调用 mapper.map(rows) 时执行。没有创建线程的代码，它不会自动异步。Lambda 提供行为，调用方决定执行时机。

</details>

**English checkpoint:** “The mapper converts each database row into a typed value. Lambdas describe behavior; they do not imply parallel execution.”
