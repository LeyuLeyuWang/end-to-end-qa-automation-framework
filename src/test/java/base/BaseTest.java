package base;

import config.ConfigManager;
import driver.WebDriverFactory;
import listeners.TestListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import pages.LoginPage;
import pages.ProductsPage;

@Listeners(TestListener.class)
public abstract class BaseTest {
    private final ThreadLocal<WebDriver> drivers = new ThreadLocal<>();
    private final ThreadLocal<LoginPage> loginPages = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void openBrowser() {
        drivers.set(WebDriverFactory.createDriver(ConfigManager.browser()));
        loginPages.set(new LoginPage(getDriver()).open());
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        try {
            if (getDriver() != null) getDriver().quit();
        } finally {
            drivers.remove();
            loginPages.remove();
        }
    }

    public WebDriver getDriver() { return drivers.get(); }
    protected LoginPage loginPage() { return loginPages.get(); }

    protected ProductsPage loginAsStandardUser() {
        loginPage().login(ConfigManager.username(), ConfigManager.password());
        return new ProductsPage(getDriver()).awaitLoaded();
    }
}

