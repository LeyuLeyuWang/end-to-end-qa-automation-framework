package api;

import api.models.Booking;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

public final class BookingClient {
    private final ApiTrace trace;
    public BookingClient(ApiTrace trace) { this.trace = trace; }

    public Response create(Booking booking) {
        return ApiRequests.create(trace).body(booking).post("/booking");
    }
    public Response createInvalid(Map<String, Object> body) {
        return ApiRequests.create(trace).body(body).post("/booking");
    }
    public Response get(int id) {
        return ApiRequests.create(trace).get("/booking/{id}", id);
    }
    public Response find(String firstname, String lastname) {
        return ApiRequests.create(trace).queryParam("firstname", firstname)
                .queryParam("lastname", lastname).get("/booking");
    }
    public Response update(int id, Booking booking, String token) {
        return authorized(token).body(booking).put("/booking/{id}", id);
    }
    public Response patch(int id, Map<String, Object> changes, String token) {
        return authorized(token).body(changes).patch("/booking/{id}", id);
    }
    public Response delete(int id, String token) {
        return authorized(token).delete("/booking/{id}", id);
    }
    private RequestSpecification authorized(String token) {
        var request = ApiRequests.create(trace);
        return token == null ? request : request.cookie("token", token);
    }
}

