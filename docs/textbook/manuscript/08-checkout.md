# 08 结账：业务流程、金额与负向输入

> 本章目标：读懂 CheckoutTest 与 CheckoutPage，理解状态机、独立计算和观测边界。

## 8.1 先把页面当成状态机

结账可以建模成 Cart → Information → Overview → Complete。合法信息允许从 Information 到 Overview；缺失必填信息应停在 Information；取消从 Information 回 Cart；点击 Finish 才完成。这比记忆四个按钮更接近业务模型。

{{source:src/test/java/tests/CheckoutTest.java}}

openCheckout 是该类的准备助手：登录、添加背包、打开购物车、开始结账。它没有 @Test，所以不会独立计数。private 表示它是此类实现细节；同一个前置步骤重复出现时抽出来，比复制四遍更清楚。

## 8.2 成功结账测试验证了哪一段旅程

customerCanCompleteCheckout 填入虚构信息，等待 Overview，核对背包名称，完成后检查感谢文案和完成 URL。它覆盖演示 UI 的流程闭环。没有支付接口、真实订单落库或邮件断言，不能说验证了“完整真实支付”。

先核对 Overview 商品再 Finish 很重要：若用户买的是错误商品，最后感谢文案也可能完全正常。流程末端一个成功提示不是整个过程所有细节正确的证据。

## 8.3 缺失字段测试为什么是四次

missingCheckoutFields 提供空 firstName、空 lastName、空 postalCode、全部空四种输入。一个测试方法由这四行驱动。每次都重新登录并准备购物车，所以不依赖上一行的错误状态。

expected 在全部为空时是 First Name is required，体现当前校验优先级。它并不证明系统同时呈现全部缺失字段。errorMessage 核对解释；isOnInformation 核对状态未前进，二者合起来比仅检查红色区域更有用。

clear 后 sendKeys 空字符串与保留旧输入不同。CheckoutPage.fill 每次清空再输入，使数据行里的空串真正在页面上表示空字段。

## 8.4 金额测试的三层依据

第一层是商品身份：背包和车灯都在。第二层是已知单价：9.99 和 29.99。第三层是关系：subtotal 等于 itemPrices 求和，tax 不为负，total 等于 subtotal + tax。

这里 reduce 的零值是 BigDecimal.ZERO，加法每次返回新 BigDecimal。compareTo(...)=0 验证数值相等，避免纯 scale 差异导致数值一致却失败。

这组断言能发现遗漏商品、错误的已知价格、错误小计、负税额和总额算术不一致。但它没有独立计算期望税额，也没有测试折扣、运费、多币种或数量大于 1 的金额。以当前范围讲清楚，比宣称“覆盖全部结账逻辑”更可信。

## 8.5 页面解析代码有什么假设

{{source:src/main/java/pages/CheckoutPage.java}}

amount 从显示文字的 `$` 之后取数，再构造 BigDecimal。它依赖该演示页面使用美元符号和可解析数字格式。若页面改成 `1,000.00` 或其他货币表示，解析可能需要修改。失败可能来自测试解析假设过时，不一定来自账单算错。

CheckoutPage 同时代表结账的多个步骤。awaitInformation/awaitOverview/finish 负责等待不同状态。更大型应用可以拆成多个页面类型，让不合时宜的操作在类型层面更难调用；当前保持一个类便于入门，但仍需调用者遵守流程。你可以创建 CheckoutPage 对象，却不代表浏览器已经在任何结账页面——构造对象和验证页面状态是两回事。

## 8.6 取消也属于业务功能

cancellingInformationReturnsToCart 不是“顺手测试返回按钮”。它验证用户退出流程时购物车仍保留背包。如果取消误清空购物车，用户会丢失之前的操作。负向与替代路径通常藏着这类体验缺陷。

<details><summary>自测：subtotal=39.98、tax=100、total=139.98，会通过当前金额关系断言吗？</summary>

如果商品单价正确，这些金额满足 tax 非负和 total=subtotal+tax，所以当前关系检查可能通过。这正说明缺少独立税率依据。增强时应先取得税率/舍入需求，再写预期，不要凭空编造规则。

</details>

**English checkpoint:** “The checkout tests validate the demo UI flow and displayed totals. They check arithmetic consistency, but they do not independently verify tax policy or real payment processing.”
