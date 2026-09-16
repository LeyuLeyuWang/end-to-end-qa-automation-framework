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

