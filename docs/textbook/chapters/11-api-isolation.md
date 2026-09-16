# 11 API 数据所有权：失败也要收拾现场

> 本章目标：理解 fixture、唯一数据、先取得清理能力、异常保留和日志脱敏。

## 11.1 为什么测试代码也要对环境负责

公共服务被多人使用。测试不能假设某条现有预约属于自己，更不能为了“环境干净”删除所有预约。可靠做法是创建自己的数据，记录标记与 ID，仅清理自己的资源。


<p class="source-ref">原代码 · <a href="../source.html#s-ff7145596aa0-L1" target="_blank" rel="noopener">src/test/java/api/BookingData.java · L1–L14</a></p>

```java
package api;

import api.models.Booking;
import api.models.BookingDates;
import java.util.UUID;

final class BookingData {
    private BookingData() { }
    static Booking fresh() {
        return new Booking("QA-" + UUID.randomUUID(), "Portfolio", 123, true,
                new BookingDates("2027-02-10", "2027-02-12"), "Breakfast");
    }
}

```


firstname 带 QA- 和 UUID，降低碰撞概率并帮助追踪。日期固定是为了避免测试行为随当天变化；当前服务接受这些演示日期。若服务未来加入日期时效校验，这个前提需要调整。随机化越多不一定越好：唯一身份用随机值，关键断言输入尽量明确。

## 11.2 BaseApiTest 的状态属于单次调用


<p class="source-ref">原代码 · <a href="../source.html#s-1c3456fbfdb4-L1" target="_blank" rel="noopener">src/test/java/api/BaseApiTest.java · L1–L98</a></p>

```java
package api;

import api.models.AuthRequest;
import api.models.Booking;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.*;

public abstract class BaseApiTest {
    protected AuthClient auth;
    protected BookingClient bookings;
    private ApiTrace trace;
    private String token;
    private final Map<Integer, String> owned = new LinkedHashMap<>();

    @BeforeMethod(alwaysRun = true)
    public void prepareApi() {
        trace = new ApiTrace();
        auth = new AuthClient(trace);
        bookings = new BookingClient(trace);
        token = null;
        owned.clear();
    }

    protected String token() {
        if (token == null) {
            Response response = auth.authenticate(new AuthRequest(ApiConfig.username(), ApiConfig.password()));
            response.then().statusCode(200).contentType(ContentType.JSON);
            token = response.jsonPath().getString("token");
            assertNotNull(token, "Authentication must return a token");
            assertFalse(token.isBlank(), "Authentication token must not be blank");
        }
        return token;
    }

    protected Response createTracked(Booking booking) {
        // Acquire cleanup credentials before creating shared-service data.
        token();
        Response response = bookings.create(booking);
        response.then().statusCode(200).contentType(ContentType.JSON);
        Integer id = response.jsonPath().get("bookingid");
        if (id != null && id > 0) {
            owned.put(id, booking.firstname());
        }
        assertNotNull(id, "Created booking must have an ID");
        assertTrue(id > 0, "Created ID must be positive");
        return response;
    }

    protected int createBooking(Booking booking) {
        return createTracked(booking).jsonPath().getInt("bookingid");
    }

    protected void assertBooking(Response response, Booking expected) {
        response.then().statusCode(200).contentType(ContentType.JSON);
        assertEquals(response.as(Booking.class), expected, "All booking fields must match");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupApi(ITestResult result) {
        Throwable cleanupFailure = null;
        try {
            for (var entry : owned.entrySet()) {
                try {
                    Response existing = bookings.get(entry.getKey());
                    if (existing.statusCode() == 404) { continue; }
                    existing.then().statusCode(200);
                    assertEquals(existing.jsonPath().getString("firstname"), entry.getValue(),
                            "Booking ownership changed; refusing to delete ID " + entry.getKey());
                    bookings.delete(entry.getKey(), token()).then().statusCode(201);
                    bookings.get(entry.getKey()).then().statusCode(404);
                } catch (Throwable failure) {
                    if (cleanupFailure == null) { cleanupFailure = failure; }
                    else { cleanupFailure.addSuppressed(failure); }
                }
            }
        } finally {
            if (!result.isSuccess() || cleanupFailure != null) {
                Reporter.log("API diagnostics (authentication bodies and headers omitted):\n" + trace.dump(), true);
            }
            owned.clear();
            token = null;
        }
        if (cleanupFailure != null) {
            if (result.getThrowable() != null) {
                result.getThrowable().addSuppressed(cleanupFailure);
            } else {
                throw new AssertionError("API test data cleanup failed", cleanupFailure);
            }
        }
    }
}
```


prepareApi 每次新建 ApiTrace 与客户端，token 设 null，owned 清空。owned 是 ID → firstname 的 LinkedHashMap。LinkedHashMap 保持插入次序，但这不意味着业务依赖清理顺序。当前类字段不是 ThreadLocal，所以 API 仍按串行设计。

token() 使用延迟获取：第一次需要时认证并验证，后续在同一次测试内复用。每次测试又重置，避免跨用例共享认证状态。鉴权客户端返回 Response，基类在这里添加“为了准备有效数据必须成功认证”的断言。

## 11.3 为什么先拿 Token，再创建资源

createTracked 的第一行先 token()。虽然公开创建接口不要求 Token，但删除需要。如果先创建再发现凭据无效，测试可能留下无法清理的数据。提前证明有清理能力，是把环境责任放到设计里。

收到创建响应后，先核对状态和媒体类型，再解析 ID；得到合法 ID 后立刻登记，随后测试体继续其他断言。这样即便后续字段比较失败，已知资源仍能清理。

这不是无条件“零残留保证”。例如服务器创建成功但连接中断、返回了无法解析的 body，客户端可能不知道 ID，无法按 ID 清理。真实系统可使用幂等键、可查询运行标记或专用测试环境改善；当前实现没有这些能力。

## 11.4 清理不等于盲目 DELETE

cleanupApi 遍历 owned。GET 为 404 表示已经没有资源，可以继续；其他情况要求 200，再检查 firstname 与登记值一致。只有检查通过才删除，之后再 GET 确认 404。

这层检查降低误删概率，但不是安全边界或原子操作：检查到删除之间仍可能发生变化，firstname 也不是密码。它是一种公共演示环境的防护性清理策略，不是服务器端所有权授权。

如果以后用例专门修改 firstname，当前 owned 的预期也要随之设计，否则会拒绝清理。这就是为什么当前 PUT/PATCH 保留该标记。理解隐含约束比只知道 Map.put 更重要。

## 11.5 清理失败为什么不能吞掉

每条资源清理都独立 try/catch，因此一条失败不会阻止尝试其他资源。多个失败通过 suppressed 保留。测试体已失败时，把清理错误附加到原异常；测试体通过但清理失败时，抛 AssertionError，让构建反映后置问题。

所以“全部业务断言通过”仍可能伴随配置/清理失败。排查时要看完整构建状态，不能只盯一个通过数。finally 中清空内存记录不表示外部资源一定已经删掉，它只是释放本地状态；错误仍需从报告处理。

## 11.6 ApiTrace 是过滤器，不是另一个 HTTP 客户端


<p class="source-ref">原代码 · <a href="../source.html#s-ce2f240b3d14-L1" target="_blank" rel="noopener">src/main/java/api/ApiTrace.java · L1–L40</a></p>

```java
package api;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.util.ArrayList;
import java.util.List;

/** Per-test, in-memory diagnostics. Never records cookies, headers or authentication bodies. */
public final class ApiTrace implements Filter {
    private final List<String> entries = new ArrayList<>();

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec, FilterContext context) {
        boolean auth = request.getURI().split("\\?")[0].endsWith("/auth");
        Object body = request.getBody();
        String requestText = request.getMethod() + " " + request.getURI()
                + "\nrequest body: " + (auth ? "[REDACTED]" : shorten(String.valueOf(body)));
        try {
            Response response = context.next(request, responseSpec);
            entries.add(requestText + "\nresponse: " + response.statusCode()
                    + " " + response.contentType() + "\n"
                    + (auth ? "[REDACTED]" : shorten(response.asString())));
            return response;
        } catch (RuntimeException failure) {
            entries.add(requestText + "\ntransport failed: " + failure.getClass().getSimpleName());
            throw failure;
        }
    }

    private static String shorten(String value) {
        return value.length() > 2000 ? value.substring(0, 2000) + "...[truncated]" : value;
    }

    public String dump() { return String.join("\n\n", entries); }
}

```


filter 接到请求描述、响应描述和上下文。context.next 把请求继续传下去并取得响应；过滤器围绕它记录诊断。没有 next 就可能没有真正发送。这里记录方法、URI、body、状态、媒体类型与截断响应，只在测试/清理失败时由基类输出。

/auth 的请求与响应 body 全部隐藏，避免密码和 Token 出现在诊断。Header 与 Cookie 不记录。AuthRequest.toString 也重写脱敏，但真正 JSON 序列化仍然需要凭据，因此不能把 toString 脱敏当成整个系统的秘密保护。

当前不是万能脱敏器：非 auth URI 或 body 中如果未来放入敏感字段，并不会自动识别。它也是失败日志，不是已实现的 Allure HTTP 请求附件。项目只直接为 Web 失败添加 PNG 附件。

## 11.7 探针是在测试测试设施

ApiFailureProbe 创建自己的预约，保存 ID 后故意 fail。预期构建失败，但 cleanupApi 仍运行。独立读取确认已删除，才能验证失败路径清理，而不是只凭代码看起来有 finally。

探针与业务回归分开，避免每次正常测试都故意红。它说明框架也需要针对关键失败路径提供可验证证据，但无需为了“写更多测试”给所有简单 getter 造镜像测试。

<details markdown="1"><summary>自测：owned.clear() 执行成功，能否证明预约已删除？</summary>

不能。它只清空 Java 内存里的登记表。外部删除必须看 DELETE 与随后 GET 的结果；清理失败会单独保留。内存状态与服务器状态不能混为一谈。

</details>

**English checkpoint:** “Each API test creates uniquely identified data and deletes only resources it owns. Cleanup failures remain visible instead of being silently ignored.”
