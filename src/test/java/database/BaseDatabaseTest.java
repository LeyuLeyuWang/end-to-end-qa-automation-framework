package database;

import java.sql.SQLException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

public abstract class BaseDatabaseTest {
    protected DatabaseClient db;

    @BeforeMethod(alwaysRun = true)
    public void beginTransaction() throws SQLException { db = new DatabaseClient(); }

    @AfterMethod(alwaysRun = true)
    public void rollbackTransaction() throws SQLException {
        if (db != null) {
            try { db.close(); }
            finally { db = null; }
        }
    }
}

