package listeners;

import base.BaseTest;
import config.ConfigManager;
import io.qameta.allure.Allure;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.Reporter;
import utils.ScreenshotUtils;

/** Capture immediately after failure, before @AfterMethod quits the browser. */
public final class TestListener implements IInvokedMethodListener {
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod() && result.getInstance() instanceof BaseTest) {
            Allure.label("browser", ConfigManager.browser());
            Allure.label("headless", Boolean.toString(ConfigManager.headless()));
            // This data provider includes a password: redact the stored JSON, not just its display.
            if (result.getMethod().getMethodName().equals("invalidLoginIsRejected")) {
                Allure.getLifecycle().updateTestCase(testCase -> {
                    testCase.getParameters().stream()
                            .filter(parameter -> parameter.getName().equals("arg2")
                                    || parameter.getName().equals("password"))
                            .forEach(parameter -> parameter.setValue("[REDACTED]"));
                });
            }
        }
        if (result.getStatus() != ITestResult.FAILURE
                || result.getAttribute("screenshot") != null
                || !(result.getInstance() instanceof BaseTest test)
                || test.getDriver() == null) {
            return;
        }
        try {
            String name = result.getTestClass().getRealClass().getSimpleName()
                    + "." + result.getMethod().getMethodName();
            String path = ScreenshotUtils.capture(test.getDriver(), name).toString();
            result.setAttribute("screenshot", path);
            try (var screenshot = Files.newInputStream(Path.of(path))) {
                Allure.addAttachment("Failure screenshot", "image/png", screenshot, ".png");
            }
            Reporter.log("Failure screenshot: " + path, true);
        } catch (Exception screenshotFailure) {
            // Preserve the original test failure rather than replacing it.
            Reporter.log("Screenshot capture failed: " + screenshotFailure.getMessage(), true);
        }
    }
}

