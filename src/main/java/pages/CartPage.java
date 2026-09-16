package pages;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtils;

public final class CartPage {
    private static final By ITEMS = By.cssSelector(".cart_item");
    private static final By BADGE = By.cssSelector(".shopping_cart_badge");
    private final WebDriver driver;
    private final WaitUtils wait;

    public CartPage(WebDriver driver) {
        this.driver = driver;
        wait = new WaitUtils(driver);
    }

    public CartPage awaitLoaded() {
        wait.urlContains("/cart.html");
        wait.visible(By.cssSelector(".cart_list"));
        return this;
    }

    public List<String> names() {
        return driver.findElements(By.cssSelector(".cart_item .inventory_item_name"))
                .stream().map(element -> element.getText()).toList();
    }

    public int itemCount() { return driver.findElements(ITEMS).size(); }

    public int quantity(Product product) {
        return driver.findElements(ITEMS).stream()
                .filter(item -> item.findElement(By.cssSelector(".inventory_item_name"))
                        .getText().equals(product.displayName()))
                .findFirst().map(item -> Integer.parseInt(
                        item.findElement(By.cssSelector(".cart_quantity")).getText()))
                .orElseThrow(() -> new IllegalStateException("Product absent from cart: " + product));
    }

    public void remove(Product product) {
        By removeButton = By.id("remove-" + product.id());
        wait.clickable(removeButton).click();
        wait.absent(removeButton);
    }

    public boolean hasBadge() { return !driver.findElements(BADGE).isEmpty(); }

    public ProductsPage continueShopping() {
        wait.clickable(By.id("continue-shopping")).click();
        return new ProductsPage(driver).awaitLoaded();
    }

    public CheckoutPage checkout() {
        wait.clickable(By.id("checkout")).click();
        return new CheckoutPage(driver).awaitInformation();
    }
}

