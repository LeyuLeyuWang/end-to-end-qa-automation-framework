package api;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.specification.RequestSpecification;

final class ApiRequests {
    private ApiRequests() { }

    static RequestSpecification create(ApiTrace trace) {
        return RestAssured.given()
                .baseUri(ApiConfig.baseUrl())
                .contentType(ContentType.JSON).accept("application/json")
                .config(RestAssuredConfig.config()
                        .objectMapperConfig(new ObjectMapperConfig(ObjectMapperType.JACKSON_2))
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", 15000)
                                .setParam("http.socket.timeout", 15000)
                                .setParam("http.connection-manager.timeout", 15000L)))
                .filter(trace);
    }
}
