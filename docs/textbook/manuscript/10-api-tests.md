# 10 API 用例逐读：返回正确还不够

> 本章目标：读懂 BookingApiTest 的 12 次执行，掌握写后读取、部分更新保留和拒绝后状态检查。

{{source:src/test/java/api/BookingApiTest.java}}

## 10.1 创建响应与可读取性是两个问题

createReturnsCompleteBooking 先构造 expected，调用 createTracked，再比较响应中 booking 与 expected。createTracked 已检查状态码、Content-Type 和正数 bookingid。测试体继续证明响应里的业务字段正确。

createdBookingCanBeFetched 则创建后发另一个 GET，并比较完整对象。这能发现一种常见缺陷：服务在创建响应里原样回显请求，看起来成功，却没有把资源放入可读取状态。

注意措辞：GET 可读证明资源通过该服务可观察，不直接证明底层一定提交了哪张数据库表。我们无法访问 Restful Booker 后端数据库，不能把外部观察说成内部存储证据。

## 10.2 PUT：核对更新响应，再独立读取

putPersistsUpdatedFields 使用同一个 firstname、新 lastname、价格、depositpaid、日期和 additionalneeds 建立 updated。先比较 PUT 响应，再 GET 比较。保留 firstname 与清理的所有权标记有关，第 11 章解释。

为什么不只看 PUT 返回 200？错误实现可能返回 200 却忽略某个字段。为什么不只比 PUT body？它可能只回显新 body 而未更新真实可读资源。第二次 GET 增加了对外部状态的独立观察。

方法名里的 persists 以随后 GET 为证据，不是数据库事务级的持久性测试，也没有模拟服务重启。

## 10.3 PATCH：小改动也要比较完整对象

patchPreservesUnchangedFields 只发送 totalprice 和 additionalneeds，expected 却包含全部字段。这样能发现 PATCH 意外清空 lastname、日期或 depositpaid。

Map.of 适合临时建立部分 JSON 字段；record 适合完整契约模型。不是所有请求都必须同一种 Java 类型。这里 `Map<String,Object>` 的值可能有整数与字符串，也说明编译器无法验证字段名是否拼错，实际测试仍有价值。

## 10.4 DELETE：动作结果与资源状态

deletedBookingCannotBeFetched 检查 DELETE 201，随后 GET 404 与 Not Found 文本。清理阶段再次 GET 看到 404 会接受“已经删除”，避免把业务测试自身完成的删除当清理失败。

nonexistentBookingReturns404 使用 -1，因为此演示创建正数 ID。它不删除别人的随机资源，也不依赖某个正数 ID 永远不存在。选数据时要考虑共享环境，不要为了方便拿公共列表第一条做删除。

## 10.5 负向测试不止一层

missingAuthenticationCannotModifyBooking 使用 PUT/PATCH/DELETE 三行 DataProvider。每行先创建自己的预约，再发不带 Token 的修改，断言 403、Forbidden，最后 GET 比较 original。

关键是请求对象每次新建，null Token 确实没有继承之前用于清理的 Cookie。否则测试可能以为在测匿名操作，实际带了认证，导致假失败或错误解释。

invalidTokenCannotUpdateBooking 测一个明确错误的 Token，和完全没有 Token 不同。它只覆盖 PATCH 的该情形，不是三个 HTTP 方法的全部错误 Token 矩阵。

## 10.6 已知不理想行为也要标清

missingRequiredPayloadIsRejected 发送空对象，期望 500 和 Internal Server Error。它属于 characterization：记录该演示服务当前已知行为。通常我们希望服务对客户端无效输入提供明确的客户端错误，但不能在没有修改服务的前提下把测试预期随意换成理想状态。

如果面试问“为什么测 500”，应回答这是第三方演示服务的已知行为，并说明生产 API 更需要审查输入校验与错误契约。不能回答“无效输入就应该返回 500”。

## 10.7 筛选测试为什么 contains，而不是结果只有一条

nameFilterFindsCreatedBooking 用 UUID firstname 和固定 lastname 查找，断言返回 bookingid 列表包含自己刚创建的 ID。公共服务可能有其他数据，测试不应该假设整个服务只有自己一个用户。

唯一化名称降低误匹配概率，但 contains 的断言也没有验证“绝不返回不匹配记录”。如果需要严格测试过滤精度，应准备可控匹配与不匹配数据，再核对排除行为。目前只证明可以找到自己创建的资源。

## 10.8 数量复核

BookingApiTest 是 10 个方法，其中无认证方法展开为 3 次，所以共 12 次。AuthApiTest 2 次，总 API 14 次。字段比较很多，不把每个字段断言计作一个独立测试场景。

<details><summary>自测：PATCH 只改价格，响应中价格正确但 lastname 变成 null，当前测试会发现吗？</summary>

会。expected 保留 original.lastname，assertBooking 反序列化整个 Booking 并做值比较，因此未指定字段意外改变也会失败。随后 GET 再提供一次对可读状态的验证。

</details>

**English checkpoint:** “After PUT and PATCH, I perform a GET to verify the observable update. For PATCH, I compare the complete object to ensure unspecified fields remain unchanged.”
