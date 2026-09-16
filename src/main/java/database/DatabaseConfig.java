package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConfig {
    private DatabaseConfig() { }

    private static String setting(String name, String fallback) {
        return System.getProperty(name, System.getenv().getOrDefault(name, fallback));
    }

    public static Connection connect() throws SQLException {
        String host = setting("DB_HOST", "localhost");
        String port = setting("DB_PORT", "55432");
        String database = setting("DB_NAME", "qa_fixture");
        Properties properties = new Properties();
        properties.setProperty("user", setting("DB_USER", "qa_local"));
        properties.setProperty("password", setting("DB_PASSWORD", "qa_local_only"));
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "15");
        try {
            return DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database,
                    properties);
        } catch (SQLException failure) {
            throw new SQLException("Cannot connect to local DB fixture at " + host + ":" + port
                    + "/" + database + ". Start docker compose -f docker/docker-compose.yml up -d --wait"
                    + " and check DB_* settings.", failure.getSQLState(), failure);
        }
    }
}

