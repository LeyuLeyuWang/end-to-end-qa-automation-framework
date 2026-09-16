package api;

import api.models.AuthRequest;
import io.restassured.http.ContentType;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AuthApiTest extends BaseApiTest {
    @Test(groups = {"api", "regression", "smoke"})
    public void validCredentialsReturnToken() {
        assertFalse(token().isBlank());
    }

    @Test(groups = {"api", "regression", "negative"})
    public void invalidCredentialsDoNotReturnToken() {
        var response = auth.authenticate(new AuthRequest(ApiConfig.username(), "incorrect-password"));
        response.then().statusCode(200).contentType(ContentType.JSON);
        assertEquals(response.jsonPath().getString("reason"), "Bad credentials");
        assertNull(response.jsonPath().get("token"), "Invalid credentials must not receive a token");
    }
}

