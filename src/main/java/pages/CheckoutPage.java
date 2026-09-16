package pages;

import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtils;

public final class CheckoutPage {
    private static final By FIRST_NAME = By.id("first-name");
    private static final By LAST_NAME = By.id("last-name");
    private static final By POSTAL_CODE = By.id("postal-code");
    private final WebDriver driver;
    private final WaitUtils wait;

    public CheckoutPage(WebDriver driver) {
        this.driver = driver;
        wait = new WaitUtils(driver);
    }

    public CheckoutPage awaitInformation() {
        wait.urlContains("/checkout-step-one.html");
        wait.visible(FIRST_NAME);
        return this;
    }

    public void submitInformation(String firstName, String lastName, String postalCode) {
        fill(FIRST_NAME, firstName);
        fill(LAST_NAME, lastName);
        fill(POSTAL_CODE, postalCode);
        wait.clickable(By.id("continue")).click();
    }

    private void fill(By locator, String value) {
        var input = wait.visible(locator);
        input.clear();
        input.sendKeys(value);
    }

    public CheckoutPage awaitOverview() {
        wait.urlContains("/checkout-step-two.html");
        wait.visible(By.cssSelector(".summary_total_label"));
        return this;
    }

    public List<String> names() {
        return driver.findElements(By.cssSelector(".cart_item .inventory_item_name"))
                .stream().map(element -> element.getText()).toList();
    }

    public List<BigDecimal> itemPrices() {
        return driver.findElements(By.cssSelector(".inventory_item_price")).stream()
                .map(element -> new BigDecimal(element.getText().replace("$", ""))).toList();
    }

    public BigDecimal subtotal() { return amount(".summary_subtotal_label"); }
    public BigDecimal tax() { return amount(".summary_tax_label"); }
    public BigDecimal total() { return amount(".summary_total_label"); }

    private BigDecimal amount(String selector) {
        String text = wait.visible(By.cssSelector(selector)).getText();
        return new BigDecimal(text.substring(text.indexOf('$') + 1));
    }

    public void finish() {
        wait.clickable(By.id("finish")).click();
        wait.urlContains("/checkout-complete.html");
        wait.visible(By.cssSelector("[data-test='complete-header']"));
    }

    public String completionMessage() {
        return wait.visible(By.cssSelector("[data-test='complete-header']")).getText();
    }

    public boolean isComplete() {
        return driver.getCurrentUrl().endsWith("/checkout-complete.html");
    }

    public String errorMessage() {
        return wait.visible(By.cssSelector("[data-test='error']")).getText();
    }

    public boolean isOnInformation() {
        return driver.getCurrentUrl().endsWith("/checkout-step-one.html");
    }

    public CartPage cancelInformation() {
        wait.clickable(By.id("cancel")).click();
        return new CartPage(driver).awaitLoaded();
    }
}

