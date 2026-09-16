package pages;

import config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtils;

public final class LoginPage {
    private static final By USERNAME = By.id("user-name");
    private static final By PASSWORD = By.id("password");
    private static final By LOGIN = By.id("login-button");
    private static final By ERROR = By.cssSelector("[data-test='error']");
    private final WebDriver driver;
    private final WaitUtils wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        wait = new WaitUtils(driver);
    }

    public LoginPage open() {
        driver.get(ConfigManager.baseUrl());
        wait.visible(USERNAME);
        return this;
    }

    public void login(String username, String password) {
        var usernameInput = wait.visible(USERNAME);
        usernameInput.clear();
        usernameInput.sendKeys(username);
        var passwordInput = wait.visible(PASSWORD);
        passwordInput.clear();
        passwordInput.sendKeys(password);
        wait.clickable(LOGIN).click();
    }

    public String errorMessage() {
        return wait.visible(ERROR).getText();
    }

    public boolean isDisplayed() {
        return !driver.getCurrentUrl().contains("/inventory.html")
                && driver.findElements(LOGIN).stream().anyMatch(element -> element.isDisplayed());
    }
}
