package tests;

import base.BaseTest;
import java.util.List;
import org.testng.annotations.Test;
import pages.Product;
import static org.testng.Assert.*;

public class CartTest extends BaseTest {
    @Test(groups = {"web", "smoke", "regression"})
    public void addedProductAppearsInCart() {
        var products = loginAsStandardUser();
        products.add(Product.BACKPACK);
        assertEquals(products.badgeCount(), 1);
        var cart = products.openCart();
        assertEquals(cart.itemCount(), 1);
        assertEquals(cart.names(), List.of(Product.BACKPACK.displayName()));
        assertEquals(cart.quantity(Product.BACKPACK), 1);
    }

    @Test(groups = {"web", "smoke", "regression"})
    public void removedProductDisappearsFromCart() {
        var products = loginAsStandardUser();
        products.add(Product.BACKPACK);
        var cart = products.openCart();
        cart.remove(Product.BACKPACK);
        assertEquals(cart.itemCount(), 0);
        assertFalse(cart.hasBadge(), "Empty cart should have no count badge");
    }

    @Test(groups = {"web", "regression"})
    public void multipleProductsAppearInCart() {
        var products = loginAsStandardUser();
        products.add(Product.BACKPACK);
        products.add(Product.BIKE_LIGHT);
        assertEquals(products.badgeCount(), 2);
        var cart = products.openCart();
        assertEquals(cart.itemCount(), 2);
        assertEquals(cart.names().stream().sorted().toList(),
                List.of(Product.BACKPACK.displayName(), Product.BIKE_LIGHT.displayName()).stream().sorted().toList());
        assertEquals(cart.quantity(Product.BACKPACK), 1);
        assertEquals(cart.quantity(Product.BIKE_LIGHT), 1);
    }

    @Test(groups = {"web", "regression"})
    public void removingOneProductPreservesTheOther() {
        var products = loginAsStandardUser();
        products.add(Product.BACKPACK);
        products.add(Product.BIKE_LIGHT);
        var cart = products.openCart();
        cart.remove(Product.BACKPACK);
        assertEquals(cart.names(), List.of(Product.BIKE_LIGHT.displayName()));
        assertEquals(cart.itemCount(), 1);
        assertEquals(cart.continueShopping().badgeCount(), 1);
    }

    @Test(groups = {"web", "regression"})
    public void productCanBeRemovedFromCatalog() {
        var products = loginAsStandardUser();
        products.add(Product.BACKPACK);
        products.remove(Product.BACKPACK);
        assertEquals(products.badgeCount(), 0);
        var cart = products.openCart();
        assertEquals(cart.itemCount(), 0);
        assertFalse(cart.hasBadge());
    }

    @Test(groups = {"web", "regression"})
    public void continueShoppingPreservesCart() {
        var products = loginAsStandardUser();
        products.add(Product.BACKPACK);
        var returnedProducts = products.openCart().continueShopping();
        assertTrue(returnedProducts.isLoaded());
        assertEquals(returnedProducts.badgeCount(), 1);
        assertEquals(returnedProducts.openCart().names(), List.of(Product.BACKPACK.displayName()));
    }
}


