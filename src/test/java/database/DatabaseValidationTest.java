package database;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DatabaseValidationTest extends BaseDatabaseTest {
    private long seedUser() throws SQLException {
        var ids = db.query("SELECT id FROM users WHERE email = ?", row -> row.getLong("id"), "seed@example.test");
        assertEquals(ids.size(), 1, "Fixture must contain exactly one seeded user");
        return ids.get(0);
    }

    private long newOrder() throws SQLException {
        return db.query("INSERT INTO orders(user_id, status, total_amount) VALUES (?, ?, ?) RETURNING id",
                row -> row.getLong("id"), seedUser(), "pending", new BigDecimal("25.50")).get(0);
    }

    @Test(groups = {"database", "regression", "smoke"})
    public void seededUserIsActive() throws SQLException {
        assertEquals(db.query("SELECT status FROM users WHERE email = ?", row -> row.getString("status"),
                "seed@example.test"), List.of("active"));
    }

    @Test(groups = {"database", "regression"})
    public void insertedOrderCanBeRead() throws SQLException {
        long id = newOrder();
        var amounts = db.query("SELECT total_amount FROM orders WHERE id = ?",
                row -> row.getBigDecimal("total_amount"), id);
        assertEquals(amounts, List.of(new BigDecimal("25.50")));
        assertEquals(db.query("SELECT status FROM orders WHERE id = ?", row -> row.getString("status"), id),
                List.of("pending"));
    }

    @Test(groups = {"database", "regression"})
    public void orderStatusCanBeUpdated() throws SQLException {
        long id = newOrder();
        assertEquals(db.update("UPDATE orders SET status = ? WHERE id = ?", "paid", id), 1);
        assertEquals(db.query("SELECT status FROM orders WHERE id = ?", row -> row.getString("status"), id),
                List.of("paid"));
    }

    @Test(groups = {"database", "regression"})
    public void orderCanBeDeleted() throws SQLException {
        long id = newOrder();
        assertEquals(db.update("DELETE FROM orders WHERE id = ?", id), 1);
        assertTrue(db.query("SELECT id FROM orders WHERE id = ?", row -> row.getLong("id"), id).isEmpty());
    }

    @Test(groups = {"database", "regression"})
    public void joinAssociatesOrderWithItsUser() throws SQLException {
        long id = newOrder();
        assertEquals(db.query(
                "SELECT u.email FROM orders o JOIN users u ON o.user_id = u.id WHERE o.id = ?",
                row -> row.getString("email"), id), List.of("seed@example.test"));
    }

    @Test(groups = {"database", "regression"})
    public void seededOrderTotalMatchesLineItems() throws SQLException {
        record Totals(BigDecimal stored, BigDecimal calculated, int lines) { }
        var totals = db.query("""
                SELECT o.total_amount, SUM(i.quantity * i.unit_price) AS calculated, COUNT(*) AS lines
                FROM orders o JOIN users u ON u.id = o.user_id
                JOIN order_items i ON i.order_id = o.id
                WHERE u.email = ?
                GROUP BY o.id, o.total_amount
                """, row -> new Totals(row.getBigDecimal("total_amount"),
                row.getBigDecimal("calculated"), row.getInt("lines")), "seed@example.test");
        assertEquals(totals.size(), 1);
        assertEquals(totals.get(0).lines(), 2);
        assertEquals(totals.get(0).stored(), new BigDecimal("39.98"));
        assertEquals(totals.get(0).calculated().compareTo(totals.get(0).stored()), 0);
    }

    @Test(groups = {"database", "regression", "negative"})
    public void nonexistentUserCannotOwnOrder() throws SQLException {
        SQLException error = expectThrows(SQLException.class, () -> db.update(
                "INSERT INTO orders(user_id, status, total_amount) VALUES (?, ?, ?)",
                -1L, "pending", new BigDecimal("1.00")));
        assertEquals(error.getSQLState(), "23503", "Expected foreign-key violation");
    }

    @Test(groups = {"database", "regression", "negative"})
    public void parameterizedInputCannotChangeQueryMeaning() throws SQLException {
        assertTrue(db.query("SELECT id FROM users WHERE email = ?", row -> row.getLong("id"),
                "' OR '1'='1").isEmpty(), "Input must be treated as a literal value");
        assertEquals(db.query("SELECT COUNT(*) AS count FROM users", row -> row.getLong("count")),
                List.of(1L), "Seed data must remain intact");
    }
}

