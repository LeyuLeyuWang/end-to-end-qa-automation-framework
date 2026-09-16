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
