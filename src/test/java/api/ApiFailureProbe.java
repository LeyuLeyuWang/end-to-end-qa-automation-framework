package api;

import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.Test;
import static org.testng.Assert.fail;

/** Explicit diagnostic only: validates failure logging and cleanup; excluded from testng.xml. */
public class ApiFailureProbe extends BaseApiTest {
    @Test(groups = "diagnostics")
    public void intentionalFailureAfterCreate() throws Exception {
        int id = createBooking(BookingData.fresh());
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "api-probe-id.txt"), Integer.toString(id));
        fail("Intentional API diagnostic failure after creating owned booking");
    }
}
