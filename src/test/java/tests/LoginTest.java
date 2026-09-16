package tests;

import base.BaseTest;
import data.TestDataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class LoginTest extends BaseTest {
    @Test(groups = {"web", "smoke", "regression"})
    public void standardUserCanLogin() {
        assertTrue(loginAsStandardUser().isLoaded(), "Login should open the Products page");
    }

    @Test(dataProvider = "rejectedLogins", dataProviderClass = TestDataProvider.class,
            groups = {"web", "regression", "negative"})
    public void invalidLoginIsRejected(String scenario, String username, String password, String expected) {
        loginPage().login(username, password);
        assertEquals(loginPage().errorMessage(), expected, scenario + ": incorrect rejection reason");
        assertTrue(loginPage().isDisplayed(), scenario + ": rejected login must remain on login page");
    }
}


