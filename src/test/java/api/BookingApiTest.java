package api;

import api.models.Booking;
import api.models.BookingDates;
import io.restassured.http.ContentType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.Map;
import static org.testng.Assert.*;

public class BookingApiTest extends BaseApiTest {
    @Test(groups = {"api", "regression", "smoke"})
    public void createReturnsCompleteBooking() {
        Booking expected = BookingData.fresh();
        var response = createTracked(expected);
        assertEquals(response.jsonPath().getObject("booking", Booking.class), expected);
    }

    @Test(groups = {"api", "regression"})
    public void createdBookingCanBeFetched() {
        Booking expected = BookingData.fresh();
        assertBooking(bookings.get(createBooking(expected)), expected);
    }

    @Test(groups = {"api", "regression"})
    public void putPersistsUpdatedFields() {
        Booking original = BookingData.fresh();
        int id = createBooking(original);
        Booking updated = new Booking(original.firstname(), "Updated", 456, false,
                new BookingDates("2027-03-01", "2027-03-05"), "Lunch");
        assertBooking(bookings.update(id, updated, token()), updated);
        assertBooking(bookings.get(id), updated);
    }

    @Test(groups = {"api", "regression"})
    public void patchPreservesUnchangedFields() {
        Booking original = BookingData.fresh();
        int id = createBooking(original);
        Booking expected = new Booking(original.firstname(), original.lastname(), 789,
                original.depositpaid(), original.bookingdates(), "Dinner");
        assertBooking(bookings.patch(id, Map.of("totalprice", 789, "additionalneeds", "Dinner"), token()), expected);
        assertBooking(bookings.get(id), expected);
    }

    @Test(groups = {"api", "regression", "smoke"})
    public void deletedBookingCannotBeFetched() {
        int id = createBooking(BookingData.fresh());
        bookings.delete(id, token()).then().statusCode(201);
        var response = bookings.get(id);
        response.then().statusCode(404);
        assertEquals(response.asString(), "Not Found");
    }

    @Test(groups = {"api", "regression", "negative"})
    public void nonexistentBookingReturns404() {
        // This demo allocates positive IDs; no other user's data is modified.
        var response = bookings.get(-1);
        response.then().statusCode(404);
        assertEquals(response.asString(), "Not Found");
    }

    @Test(groups = {"api", "regression", "negative"})
    public void missingRequiredPayloadIsRejected() {
        // Characterizes this demo's known 500 response, not a recommended production contract.
        var response = bookings.createInvalid(Map.of());
        response.then().statusCode(500);
        assertEquals(response.asString(), "Internal Server Error");
    }

    @DataProvider(name = "unauthorizedMethods")
    public Object[][] unauthorizedMethods() {
        return new Object[][] {{"PUT"}, {"PATCH"}, {"DELETE"}};
    }

    @Test(dataProvider = "unauthorizedMethods", groups = {"api", "regression", "negative"})
    public void missingAuthenticationCannotModifyBooking(String method) {
        Booking original = BookingData.fresh();
        int id = createBooking(original);
        var response = switch (method) {
            case "PUT" -> bookings.update(id, new Booking(original.firstname(), "Unauthorized",
                    999, false, original.bookingdates(), "Changed"), null);
            case "PATCH" -> bookings.patch(id, Map.of("lastname", "Unauthorized"), null);
            case "DELETE" -> bookings.delete(id, null);
            default -> throw new IllegalArgumentException(method);
        };
        response.then().statusCode(403);
        assertEquals(response.asString(), "Forbidden");
        assertBooking(bookings.get(id), original);
    }

    @Test(groups = {"api", "regression", "negative"})
    public void invalidTokenCannotUpdateBooking() {
        Booking original = BookingData.fresh();
        int id = createBooking(original);
        bookings.patch(id, Map.of("totalprice", 999), "invalid-token").then().statusCode(403);
        assertBooking(bookings.get(id), original);
    }

    @Test(groups = {"api", "regression"})
    public void nameFilterFindsCreatedBooking() {
        Booking original = BookingData.fresh();
        int id = createBooking(original);
        var response = bookings.find(original.firstname(), original.lastname());
        response.then().statusCode(200).contentType(ContentType.JSON);
        assertTrue(response.jsonPath().getList("bookingid", Integer.class).contains(id),
                "Filtered results should include the booking created by this test");
    }
}
