package data;

import config.ConfigManager;
import org.testng.annotations.DataProvider;
import pages.ProductsPage.Sort;

public final class TestDataProvider {
    private TestDataProvider() { }

    @DataProvider(name = "rejectedLogins")
    public static Object[][] rejectedLogins() {
        return new Object[][] {
            {"invalid password", ConfigManager.username(), "wrong_password",
                "Epic sadface: Username and password do not match any user in this service"},
            {"locked user", "locked_out_user", ConfigManager.password(),
                "Epic sadface: Sorry, this user has been locked out."},
            {"empty username", "", ConfigManager.password(), "Epic sadface: Username is required"},
            {"empty password", ConfigManager.username(), "", "Epic sadface: Password is required"},
            {"both empty", "", "", "Epic sadface: Username is required"}
        };
    }

    @DataProvider(name = "nameSorts")
    public static Object[][] nameSorts() {
        return new Object[][] {{Sort.NAME_ASC}, {Sort.NAME_DESC}};
    }

    @DataProvider(name = "priceSorts")
    public static Object[][] priceSorts() {
        return new Object[][] {{Sort.PRICE_ASC}, {Sort.PRICE_DESC}};
    }

    @DataProvider(name = "missingCheckoutFields")
    public static Object[][] missingCheckoutFields() {
        return new Object[][] {
            {"first name", "", "Customer", "90210", "Error: First Name is required"},
            {"last name", "Demo", "", "90210", "Error: Last Name is required"},
            {"postal code", "Demo", "Customer", "", "Error: Postal Code is required"},
            {"all fields", "", "", "", "Error: First Name is required"}
        };
    }
}

