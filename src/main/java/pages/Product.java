package pages;

/** Stable demo product identifiers, shared by product and cart pages. */
public enum Product {
    BACKPACK("sauce-labs-backpack", "Sauce Labs Backpack"),
    BIKE_LIGHT("sauce-labs-bike-light", "Sauce Labs Bike Light"),
    T_SHIRT("sauce-labs-bolt-t-shirt", "Sauce Labs Bolt T-Shirt");

    private final String id;
    private final String displayName;

    Product(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
}

