package tests;

import base.BaseTest;
import data.TestDataProvider;
import java.math.BigDecimal;
import java.util.List;
import org.testng.annotations.Test;
import pages.CheckoutPage;
import pages.Product;
import static org.testng.Assert.*;

public class CheckoutTest extends BaseTest {
    private CheckoutPage openCheckout() {
        var products = loginAsStandardUser();
        products.add(Product.BACKPACK);
        return products.openCart().checkout();
    }

    @Test(groups = {"web", "smoke", "regression"})
    public void customerCanCompleteCheckout() {
        var checkout = openCheckout();
        checkout.submitInformation("Demo", "Customer", "90210");
        checkout.awaitOverview();
        assertEquals(checkout.names(), List.of(Product.BACKPACK.displayName()));
        checkout.finish();
        assertEquals(checkout.completionMessage(), "Thank you for your order!");
        assertTrue(checkout.isComplete());
    }

    @Test(dataProvider = "missingCheckoutFields", dataProviderClass = TestDataProvider.class,
            groups = {"web", "regression", "negative"})
    public void missingRequiredInformationPreventsCheckout(
            String scenario, String firstName, String lastName, String postalCode, String expected) {
        var checkout = openCheckout();
        checkout.submitInformation(firstName, lastName, postalCode);
        assertEquals(checkout.errorMessage(), expected, "Incorrect validation for " + scenario);
        assertTrue(checkout.isOnInformation(), "Invalid input must not advance checkout: " + scenario);
    }

    @Test(groups = {"web", "regression"})
    public void overviewTotalsMatchSelectedProducts() {
        var products = loginAsStandardUser();
        products.add(Product.BACKPACK);
        products.add(Product.BIKE_LIGHT);
        var checkout = products.openCart().checkout();
        checkout.submitInformation("Demo", "Customer", "90210");
        checkout.awaitOverview();
        assertEquals(checkout.names().stream().sorted().toList(),
                List.of(Product.BACKPACK.displayName(), Product.BIKE_LIGHT.displayName()).stream().sorted().toList());
        assertEquals(checkout.itemPrices().stream().sorted().toList(),
                List.of(new BigDecimal("9.99"), new BigDecimal("29.99")));
        BigDecimal itemSum = checkout.itemPrices().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(checkout.subtotal().compareTo(itemSum), 0, "Subtotal must equal the item sum");
        assertTrue(checkout.tax().signum() >= 0, "Tax must not be negative");
        assertEquals(checkout.total().compareTo(checkout.subtotal().add(checkout.tax())), 0,
                "Total must equal subtotal plus displayed tax");
    }

    @Test(groups = {"web", "regression"})
    public void cancellingInformationReturnsToCart() {
        var cart = openCheckout().cancelInformation();
        assertEquals(cart.itemCount(), 1);
        assertEquals(cart.names(), List.of(Product.BACKPACK.displayName()));
    }
}


