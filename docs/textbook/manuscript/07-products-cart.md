# 07 商品与购物车：怎样写有辨别力的断言

> 本章目标：逐个读懂 ProductTest 和 CartTest，用“反例能否逃过断言”评估测试质量。

## 7.1 目录测试验证的是一个受控演示前提

{{source:src/test/java/tests/ProductTest.java}}

productPageShowsCatalog 核对页面加载、商品数为 6、包含背包。6 是演示目录的已知预期，不是所有电商网站的通用规则。若上游合法增加商品，它会失败，此时应核对需求变化，而不是机械认定产品坏了。

只断言 size=6 仍可能放过“六个错误商品”。再查背包能增加一些身份验证，但没有核对全部六个商品的完整详情。这不是批评它没有测尽所有事情，而是训练你准确描述覆盖边界。

## 7.2 为什么先切到反方向，再测名称排序

productsSortByName 的第一步根据目标方向选择相反方向。假如页面默认已经 A-Z，你只选一次 A-Z，即使排序按钮完全无效，结果也可能正确。先变成 Z-A，再切回 A-Z，可以证明目标排序触发了状态转换。

before 是页面当前名称列表；expected 是它的副本；Java Comparator 把副本按目标方向排序；页面操作后比较完整列表。expected 不从排序后的页面直接复制，因此不会把 actual 当自己的标准。

ArrayList 副本也有必要：Stream.toList 得到的列表不能假定可原地修改。更重要的是要保留 before，才能随后检查顺序确实改变。

这个判定使用 Java 字符串自然序作为该演示目录的预期。如果产品要求语言地区相关的排序，Java 默认比较未必就是正确业务规则。测试依据必须对应实际需求。

## 7.3 价格排序同时检查顺序与集合

priceSortChangesProductOrder 先保存名称与价格，再构造排好序的 expectedPrices。操作后核对价格列表相等、数量仍是 6、商品名称顺序变化、完整名称集合保留。

为什么两个名字列表都 sorted 后再比？这里验证的是成员和重复数量，不关心最终名称顺序。直接比较未排序列表会把合法的价格排序判错。反过来，测试排序功能时又不能只比较集合，因为集合相同不能证明顺序正确。

当前断言仍有一个值得知道的盲区：名称集合正确、价格集合正确，不保证每个名称仍绑定原来的价格。若页面把价格分配给错误商品，某些错误可能逃过。这是可提出的后续增强：捕获 `(name, price)` 成对记录。它目前没有实现，教材不会假装已有覆盖。

{{source:src/main/java/pages/ProductsPage.java#sortBy}}

排序控件用 Selenium Select 操作原生 select；selectByValue 根据选项 value 选择。等待先确认选中项，再确认列表有序。ordered 对相邻值比较，如果任何相邻对违反方向就返回 false；空列表也不视为排序完成。此等待避免只看到下拉框变化就过早断言商品区。

## 7.4 六条购物车测试，各自关心什么

{{source:src/test/java/tests/CartTest.java}}

addedProductAppearsInCart 从目录添加背包，核对 badge=1；进入购物车后核对行数、名称、quantity。三个角度分别是全局提示、实际成员和行内数量。一个 badge 不能替代实际购物车内容。

removedProductDisappearsFromCart 在购物车删除唯一商品，要求 itemCount=0 且 badge 不存在。空列表与显示数字 0 的 badge 是否等价取决于需求；本演示预期是没有 badge。

multipleProductsAppearInCart 同时添加背包与车灯，核对数量 2、名称集合和每个 quantity=1。排序名称仅为忽略购物车显示顺序，并不忽略成员身份。

removingOneProductPreservesTheOther 最体现不变量思想：从两个商品中删除背包，剩下必须是车灯，行数为 1，返回目录 badge 仍为 1。它防止“删除按钮清空了整个购物车”。

productCanBeRemovedFromCatalog 验证另一操作入口。在购物车删除成功不自动证明目录页的 remove 按钮正常，因此单独覆盖有价值。

continueShoppingPreservesCart 检查导航不会清掉购物车：回到目录后页面正确、badge 正确，再回购物车内容仍在。一个动作的后果可能跨页面，需要选合适的观察点。

## 7.5 页面方法为什么这样封装

{{source:src/main/java/pages/CartPage.java}}

quantity 不是按第 0 行读取，它按 Product.displayName 找行，避免顺序改变后读错商品。找不到会抛明确异常。remove 等删除按钮不再存在，而不是固定休眠。continueShopping 返回已等待加载的 ProductsPage，因此测试可以顺着业务步骤调用。

这些方法改善可读性，却没有把测试变成黑盒魔法。你仍要追踪每个返回对象使用同一个 driver、每个 getter 观察了什么，以及等待条件是不是足够。

<details><summary>自测：添加两个商品后只断言 badge=2，能发现两个相同商品被错误加入吗？</summary>

不能可靠发现。计数正确不表示身份正确。当前测试还比较名称集合和每个商品的 quantity。可以继续思考重复商品或同名商品会带来什么定位与断言问题。

</details>

**English checkpoint:** “For sorting, I verify order and preservation of items. For removal, I verify that the selected item disappears while the other item remains.”
