package pages;

import config.ConfigManager;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.WaitUtils;

public final class ProductsPage {
    public enum Sort {
        NAME_ASC("az"), NAME_DESC("za"), PRICE_ASC("lohi"), PRICE_DESC("hilo");
        private final String value;
        Sort(String value) { this.value = value; }
    }

    private static final By TITLE = By.cssSelector("[data-test='title']");
    private static final By NAMES = By.cssSelector(".inventory_item_name");
    private static final By PRICES = By.cssSelector(".inventory_item_price");
    private static final By SORT = By.cssSelector("[data-test='product-sort-container']");
    private static final By BADGE = By.cssSelector(".shopping_cart_badge");
    private static final By CART = By.cssSelector(".shopping_cart_link");
    private final WebDriver driver;
    private final WaitUtils wait;

    public ProductsPage(WebDriver driver) {
        this.driver = driver;
        wait = new WaitUtils(driver);
    }

    public ProductsPage awaitLoaded() {
        wait.urlContains("/inventory.html");
        wait.visible(NAMES);
        return this;
    }

    public boolean isLoaded() {
        return driver.getCurrentUrl().endsWith("/inventory.html")
                && "Products".equals(wait.visible(TITLE).getText());
    }

    public List<String> names() {
        wait.visible(NAMES);
        return driver.findElements(NAMES).stream().map(element -> element.getText()).toList();
    }

    public List<BigDecimal> prices() {
        wait.visible(PRICES);
        return driver.findElements(PRICES).stream()
                .map(element -> new BigDecimal(element.getText().replace("$", ""))).toList();
    }

    public void sortBy(Sort sort) {
        new Select(wait.clickable(SORT)).selectByValue(sort.value);
        // Wait for actual catalog ordering, not merely the dropdown selection.
        new WebDriverWait(driver, ConfigManager.waitTimeout())
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    if (!sort.value.equals(new Select(d.findElement(SORT))
                            .getFirstSelectedOption().getDomAttribute("value"))) {
                        return false;
                    }
                    if (sort == Sort.NAME_ASC || sort == Sort.NAME_DESC) {
                        return ordered(names(), sort == Sort.NAME_DESC);
                    }
                    return ordered(prices(), sort == Sort.PRICE_DESC);
                });
    }

    private static <T extends Comparable<? super T>> boolean ordered(List<T> values, boolean descending) {
        if (values.isEmpty()) { return false; }
        for (int i = 1; i < values.size(); i++) {
            int comparison = values.get(i - 1).compareTo(values.get(i));
            if (descending ? comparison < 0 : comparison > 0) { return false; }
        }
        return true;
    }

    public void add(Product product) {
        wait.clickable(By.id("add-to-cart-" + product.id())).click();
        wait.visible(By.id("remove-" + product.id()));
    }

    public void remove(Product product) {
        wait.clickable(By.id("remove-" + product.id())).click();
        wait.visible(By.id("add-to-cart-" + product.id()));
    }

    public int badgeCount() {
        var badges = driver.findElements(BADGE);
        return badges.isEmpty() ? 0 : Integer.parseInt(badges.get(0).getText());
    }

    public CartPage openCart() {
        wait.clickable(CART).click();
        return new CartPage(driver).awaitLoaded();
    }
}

