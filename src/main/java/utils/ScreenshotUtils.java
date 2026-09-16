package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public final class ScreenshotUtils {
    private ScreenshotUtils() { }

    public static Path capture(WebDriver driver, String testName) throws IOException {
        Path directory = Path.of("test-output", "screenshots");
        Files.createDirectories(directory);
        String safeName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path file = directory.resolve(safeName + "-" + Instant.now().toEpochMilli()
                + "-" + UUID.randomUUID() + ".png");
        Files.write(file, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        return file.toAbsolutePath();
    }
}
