package api;

public final class ApiConfig {
    private ApiConfig() { }
    public static String baseUrl() {
        return System.getProperty("apiBaseUrl", "https://restful-booker.herokuapp.com");
    }
    public static String username() { return System.getProperty("apiUsername", "admin"); }
    public static String password() { return System.getProperty("apiPassword", "password123"); }
}

