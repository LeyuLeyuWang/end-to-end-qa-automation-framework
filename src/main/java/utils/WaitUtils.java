package utils;

import config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Small condition helpers; page-specific conditions stay in their page objects. */
public final class WaitUtils {
    private final WebDriverWait wait;

    public WaitUtils(WebDriver driver) {
        wait = new WebDriverWait(driver, ConfigManager.waitTimeout());
    }

    public WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement clickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void urlContains(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    public void absent(By locator) {
        wait.until(ExpectedConditions.numberOfElementsToBe(locator, 0));
    }
}
