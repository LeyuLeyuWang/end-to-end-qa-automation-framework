package config;

import java.time.Duration;

/** System properties override the public demo defaults. */
public final class ConfigManager {
    private ConfigManager() { }

    public static String baseUrl() {
        return System.getProperty("baseUrl", "https://www.saucedemo.com/");
    }

    public static String username() {
        return System.getProperty("username", "standard_user");
    }

    public static String password() {
        return System.getProperty("password", "secret_sauce");
    }

    public static boolean headless() {
        String value = System.getProperty("headless", System.getenv().getOrDefault("HEADLESS", "false"));
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("headless / HEADLESS must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    public static String browser() {
        return System.getProperty("browser", "chrome");
    }

    public static Duration waitTimeout() {
        long seconds = Long.parseLong(System.getProperty("waitSeconds", "10"));
        if (seconds <= 0) {
            throw new IllegalArgumentException("waitSeconds must be positive");
        }
        return Duration.ofSeconds(seconds);
    }
}
