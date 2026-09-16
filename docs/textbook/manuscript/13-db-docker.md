# 13 数据库八个场景与 Docker 环境

> 本章目标：从表关系读到 SQL 断言，再理解镜像、容器、端口、数据卷与初始化。

## 13.1 三张表表达什么关系

{{source:docker/init/init.sql}}

users.id 是主键，email 唯一且不能为 null。orders.user_id 引用 users.id，因此订单不能指向不存在用户。order_items.order_id 引用 orders.id；ON DELETE CASCADE 让删除订单时明细一起删除。CHECK 约束限制状态、数量、金额。

```text
users 1 ────────── N orders 1 ────────── N order_items
      user_id 外键             order_id 外键
```

主键识别一行；外键维护引用完整性；UNIQUE 防止重复；NOT NULL 防止缺失；CHECK 验证允许范围。这里定义了多种约束，但业务套件没有逐一测试所有约束。存在 schema 代码不等于已有相应测试覆盖。

seed 是 1 用户、1 paid 订单、2 明细，29.99 + 9.99 = 39.98。它是已知判定依据。初始化脚本在空卷上运行，所以其中从 orders 选取的插入逻辑建立在初始 fixture 前提上，不是通用生产数据迁移脚本。

## 13.2 八个测试逐条读

{{source:src/test/java/database/DatabaseValidationTest.java}}

seededUserIsActive 通过 email 查询状态，比较 List.of("active")。列表相等同时要求结果数量和内容：空结果、两条结果或 inactive 都不能通过。

insertedOrderCanBeRead 先 newOrder 获取 INSERT RETURNING 的 ID，再按 ID 读金额和状态。它验证写入后在当前事务内可读，也验证条件选的是刚创建的订单，不依赖全表第一行。

orderStatusCanBeUpdated 先创建 pending，UPDATE 为 paid，影响行数必须为 1，再读状态。仅检查影响行数不够，因为可能写错值；仅读值也可能碰巧原本就是 paid，因此受控起点有意义。

orderCanBeDeleted 先有再删，要求影响一行，随后查询为空。删除不存在 ID 返回 0 不是这个测试想证明的成功场景。

joinAssociatesOrderWithItsUser 使用 `JOIN users u ON o.user_id=u.id`，核对新订单对应 seed email。o/u 是 SQL 表别名，不是 Java 对象。JOIN 把相关行组合，不会自动保证业务关联正确，所以要断言实际关联对象。

seededOrderTotalMatchesLineItems 用 SUM(quantity*unit_price) 聚合明细，GROUP BY 订单，让每个订单得到一个计算结果。COUNT(*) 在这条 inner join 查询里数匹配明细行；断言为 2。局部 record Totals 将三个列组织成明确值对象。测试同时要求只有一张种子订单、存储金额 39.98、计算金额一致。

nonexistentUserCannotOwnOrder 使用 -1 user_id，并用 expectThrows 捕获 SQLException。预期异常是成功证据，没抛才失败。随后检查 SQLSTATE 23503，避免把连接失败、语法错误等随便一个 SQLException 误当外键拒绝。

parameterizedInputCannotChangeQueryMeaning 用注入式字符串查询，预期空列表，再核对 seed 数量仍为 1。第二个断言不是证明所有安全风险都消失，而是确认此练习环境未被该操作破坏。

## 13.3 Docker 不是数据库本身

Image（镜像）是包含软件与启动配置的模板；Container（容器）是运行实例；Volume（数据卷）保存需要跨容器保留的数据。Compose 用 YAML 描述服务配置，避免每次手敲长命令。

{{source:docker/docker-compose.yml}}

postgres:17.11-alpine 固定数据库镜像标签。127.0.0.1:55432:5432 表示宿主机只在本机地址的 55432 端口接收连接，转给容器 5432。Java 运行在宿主机，所以默认连 55432；不是看到 PostgreSQL 就一律写 5432。

`./init:/docker-entrypoint-initdb.d:ro` 把初始化目录只读挂载到镜像识别的位置。`pgdata:/var/lib/postgresql/data` 是数据卷。因此 stop/down 后数据可能还在；重建容器不等于重建数据库。

## 13.4 为什么要健康检查

容器 running 只表示进程启动，不表示数据库已经接受连接。pg_isready 检查服务是否可连接；`up -d --wait` 等健康状态。健康不等于已验证表内容或账号权限，测试仍需要真实 SQL 证明。

Compose 中 `${DB_PORT:-55432}` 表示读取变量、缺失或空时用默认值。健康命令中的 `$$POSTGRES_USER` 用双美元避免 Compose 提前替换，让容器 shell 处理变量。这是 YAML/Compose/shell 多层解析，不是 Java 语法。

项目名仍为 qa-automation-v4，是数据库模块引入时的资源名称；它不限制 Java 项目只能停留在 V4，也没必要为版本号随意更名并造成另一套数据卷。

## 13.5 常用操作及其真实影响

```powershell
docker compose -f docker/docker-compose.yml up -d --wait
.\mvnw.cmd '-Dgroups=database' test
docker compose -f docker/docker-compose.yml ps
docker compose -f docker/docker-compose.yml down
```

down 停止并移除本项目容器/网络，保留命名卷。只有明确需要删除本项目 fixture 数据并重新初始化时才使用 down -v，它会删除对应数据卷。不能把它当日常无害清理按钮。

改 init.sql 后旧卷不会自动重跑初始化；`.env.example` 也不是 Java 自动读取配置的机制。需要同时改变 Docker 和 Java 配置时，shell 环境变量更直观。诊断连接问题按引擎 → 容器健康 → 端口 → 账号/库名 → schema 顺序检查。

<details><summary>自测：修改 init.sql 后重启容器，新增表没有出现，最可能是哪条假设错了？</summary>

误以为初始化脚本每次启动都执行。官方镜像通常仅在空数据目录初始化时运行该目录脚本；已有数据卷需要明确迁移或在允许丢弃 fixture 时重建。不要在不了解数据卷内容时直接删除。

</details>

**English checkpoint:** “Docker makes the PostgreSQL fixture reproducible. The volume persists data, so restarting a container does not rerun initialization or reset the schema.”
