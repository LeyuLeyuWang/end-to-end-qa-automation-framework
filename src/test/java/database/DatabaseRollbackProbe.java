package database;

import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.Test;
import static org.testng.Assert.fail;

/** Explicitly selected diagnostic; not included in the business suite. */
public class DatabaseRollbackProbe extends BaseDatabaseTest {
    @Test(groups = "diagnostics")
    public void intentionalFailureAfterInsert() throws Exception {
        long id = db.query("INSERT INTO users(email, status) VALUES (?, ?) RETURNING id",
                row -> row.getLong("id"), "rollback-probe@example.test", "active").get(0);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/db-probe-id.txt"), Long.toString(id));
        fail("Intentional failure: transaction must still roll back");
    }
}

