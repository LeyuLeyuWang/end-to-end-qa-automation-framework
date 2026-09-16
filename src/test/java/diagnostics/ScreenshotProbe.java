package diagnostics;

import base.BaseTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/** Opt-in diagnostic, excluded from testng.xml and normal Surefire naming patterns. */
public class ScreenshotProbe extends BaseTest {
    @DataProvider(name = "failureCases")
    public Object[][] failureCases() {
        return new Object[][] {{"first invocation"}, {"second invocation"}};
    }

    @Test(dataProvider = "failureCases", groups = "diagnostics")
    public void intentionalFailure(String scenario) {
        assertTrue(loginPage().isDisplayed());
        fail("Intentional screenshot validation: " + scenario);
    }

    @Test(groups = "diagnostics")
    public void passingTestMustNotCapture() {
        assertTrue(loginPage().isDisplayed());
    }
}
