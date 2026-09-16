# 09 HTTP 与 REST Assured：跳过页面，直接验证接口

> 本章目标：读懂请求方法、JSON、认证、序列化和链式 API。明确 Web 测试与 API 测试的不同观察面。

## 9.1 API 测试到底省略了什么

浏览器测试先操作 UI，再间接触发应用行为。API 测试直接发送 HTTP 请求，观察状态码、响应体与后续状态。它不验证按钮、布局和浏览器交互。两者可以互补，但此仓库的 API 目标是另一个独立服务，不是绕过 SauceDemo 页面调用它的后端。

一个 HTTP 请求包含方法、URL、Header 和可选 body。响应包含状态码、Header 和 body。GET 通常读取；POST 常用于创建；PUT 在这里传完整更新对象；PATCH 传部分变化；DELETE 删除。具体成功码由被测接口约定决定。

此演示创建返回 200、删除返回 201、无效认证可能返回 200 加 reason，空 payload 返回 500。它们不是教材推荐你设计生产 API 的方式。测试可以描述实际约定或已知行为，同时说明其局限。

## 9.2 Content-Type 和 Accept 分别说什么

Content-Type 描述本次发送内容的媒体类型，例如 JSON。Accept 表示希望服务器返回哪种表示。相同字符串不代表同一方向。ApiRequests 明确 `.contentType(ContentType.JSON).accept("application/json")`。

历史开发遇到过枚举 Accept 展开后与该服务不兼容的 418；当前保留精确字符串。不要把“某个框架常量看起来合理”当作线上实际请求头已经符合服务预期，应检查真正发送的内容。

{{source:src/main/java/api/ApiRequests.java}}

RestAssured.given() 建立请求描述对象；baseUri、contentType、config、filter 都在配置它。直到 get/post/put 等调用才发送请求。`.then().statusCode(200)` 属于响应验证，不是发送前设置“我希望请求变成 200”。

连接超时、socket 超时、连接管理等待是不同阶段的限制，不构成严格的“整个测试 15 秒必须结束”。一条测试还可能包含多次请求和清理。

## 9.3 Java 对象如何成为 JSON

{{source:src/main/java/api/models/Booking.java}}

{{source:src/main/java/api/models/BookingDates.java}}

BookingData 构造的 Booking 对象包含嵌套 BookingDates。Jackson 按模型把对象序列化成 JSON；响应可以反序列化回 Booking。Serialization 是 Java → 文本表示，deserialization 是文本 → Java。

教学示例，结构与模型对应：

```json
{
  "firstname": "QA-example",
  "lastname": "Portfolio",
  "totalprice": 123,
  "depositpaid": true,
  "bookingdates": {"checkin": "2027-02-10", "checkout": "2027-02-12"},
  "additionalneeds": "Breakfast"
}
```

模型的名字对应 JSON 字段，所以这里使用 firstname 而不是 firstName。totalprice 是 int，按此演示 API 的约定；不要直接据此推导生产财务金额都应该用整数或都用同一单位。

jsonPath().getString("token") 从 JSON 取指定字段；getObject("booking", Booking.class) 解析嵌套 booking；response.as(Booking.class) 解析整个 body。两种解析位置不同，因为创建响应有包装对象，而读取响应直接是预约内容。

## 9.4 客户端为什么返回 Response

{{source:src/main/java/api/BookingClient.java}}

BookingClient 封装 URL、方法、请求体和认证方式，但不内置“每次必须 200”。同一个 update 既用于合法 Token，也用于 null Token 的反向测试。如果客户端一发送就强制成功，会妨碍验证合法的 403 响应。

`get("/booking/{id}", id)` 是路径参数；find 使用 queryParam，把筛选条件放进 URL 查询部分；body 放请求体；cookie 把 Token 放 Cookie。它们位于请求不同位置。不要把 Token 默认想成 Bearer Header，此服务当前使用 token cookie。

authorized 每次先建立全新的 request，然后按 token 是否为 null 添加 Cookie。新请求对象避免上一条请求的 body 或 Cookie 意外残留。它没有修改 RestAssured 全局 baseURI 等静态配置，这是减少共享状态的设计。

## 9.5 认证和授权不是完全一样

Authentication 确认凭据能否获得身份凭证；authorization 检查某操作是否被允许。AuthApiTest 测登录凭据返回 Token；BookingApiTest 测缺少或错误 Token 能否修改预约。

{{source:src/test/java/api/AuthApiTest.java}}

validCredentialsReturnToken 的 token() 助手还检查状态码、Content-Type、非 null、非空。invalidCredentials 则检查 reason 并明确 token 不存在。200 只说明服务按其约定返回了响应，不自动等于业务认证成功。

这些用例不是完整权限矩阵：没有不同角色、租户、Token 过期或跨用户访问等覆盖。拥有一条 403 测试不能宣称做完系统安全测试。

<details><summary>自测：为什么要先验证 Content-Type，再把响应解析成 Booking？</summary>

如果代理或服务返回 HTML 错误页，直接 JSON 解析会给出误导性的格式异常。先检查响应基础契约能让失败更接近实际问题。依然需要看原始诊断，不能只凭解析失败定位服务根因。

</details>

官方查阅：[REST Assured 用法](https://github.com/rest-assured/rest-assured/wiki/Usage)。

**English checkpoint:** “I separate request construction from assertions so the same client supports positive and negative tests. I validate response contracts and then verify observable resource state.”
