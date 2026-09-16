package tests;

import base.BaseTest;
import data.TestDataProvider;
import java.util.ArrayList;
import java.util.Comparator;
import org.testng.annotations.Test;
import pages.Product;
import pages.ProductsPage.Sort;
import static org.testng.Assert.*;

public class ProductTest extends BaseTest {
    @Test(groups = {"web", "smoke", "regression"})
    public void productPageShowsCatalog() {
        var products = loginAsStandardUser();
        assertTrue(products.isLoaded(), "Products page should be loaded");
        assertEquals(products.names().size(), 6, "Demo catalog should contain six products");
        assertTrue(products.names().contains(Product.BACKPACK.displayName()), "Backpack should be listed");
    }

    @Test(dataProvider = "nameSorts", dataProviderClass = TestDataProvider.class, groups = {"web", "regression"})
    public void productsSortByName(Sort sort) {
        var products = loginAsStandardUser();
        // Exercise A-Z as a transition, not merely the default selection.
        products.sortBy(sort == Sort.NAME_ASC ? Sort.NAME_DESC : Sort.NAME_ASC);
        var before = products.names();
        var expected = new ArrayList<>(before);
        expected.sort(sort == Sort.NAME_ASC ? Comparator.naturalOrder() : Comparator.reverseOrder());
        products.sortBy(sort);
        assertEquals(products.names(), expected, "Name ordering and identities should match: " + sort);
        assertEquals(products.names().size(), 6);
        assertNotEquals(products.names(), before, "The demo catalog should change order");
    }

    @Test(dataProvider = "priceSorts", dataProviderClass = TestDataProvider.class, groups = {"web", "regression"})
    public void priceSortChangesProductOrder(Sort sort) {
        var products = loginAsStandardUser();
        var beforeNames = products.names();
        var expectedPrices = new ArrayList<>(products.prices());
        expectedPrices.sort(sort == Sort.PRICE_ASC ? Comparator.naturalOrder() : Comparator.reverseOrder());
        products.sortBy(sort);
        assertEquals(products.prices(), expectedPrices, "Prices should be ordered and preserved: " + sort);
        assertEquals(products.prices().size(), 6);
        assertNotEquals(products.names(), beforeNames, "Price order should differ from default name order");
        assertEquals(products.names().stream().sorted().toList(), beforeNames.stream().sorted().toList(),
                "Sorting must preserve the complete product name list");
    }
}


