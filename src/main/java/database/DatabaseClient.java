package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** One connection and rollback-only transaction per client. No ORM or implicit commits. */
public final class DatabaseClient implements AutoCloseable {
    @FunctionalInterface
    public interface RowMapper<T> { T map(ResultSet row) throws SQLException; }

    private final Connection connection;

    public DatabaseClient() throws SQLException {
        connection = DatabaseConfig.connect();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException failure) {
            try { connection.close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(sql, parameters);
             ResultSet rows = statement.executeQuery()) {
            List<T> result = new ArrayList<>();
            while (rows.next()) { result.add(mapper.map(rows)); }
            return result;
        }
    }

    public int update(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(sql, parameters)) {
            return statement.executeUpdate();
        }
    }

    private PreparedStatement prepare(String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            statement.setQueryTimeout(10);
            for (int i = 0; i < parameters.length; i++) { statement.setObject(i + 1, parameters[i]); }
            return statement;
        } catch (SQLException failure) {
            try { statement.close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    @Override
    public void close() throws SQLException {
        SQLException failure = null;
        try { connection.rollback(); }
        catch (SQLException rollbackFailure) { failure = rollbackFailure; }
        try { connection.close(); }
        catch (SQLException closeFailure) {
            if (failure == null) { failure = closeFailure; }
            else { failure.addSuppressed(closeFailure); }
        }
        if (failure != null) { throw failure; }
    }
}
