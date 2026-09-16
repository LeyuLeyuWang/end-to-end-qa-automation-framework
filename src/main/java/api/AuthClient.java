package api;

import api.models.AuthRequest;
import io.restassured.response.Response;

public final class AuthClient {
    private final ApiTrace trace;
    public AuthClient(ApiTrace trace) { this.trace = trace; }
    public Response authenticate(AuthRequest request) {
        return ApiRequests.create(trace).body(request).post("/auth");
    }
}

