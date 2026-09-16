# QA Automation Project V1 — Selenium Framework, POM, Waits, and Data-Driven Testing

## Purpose

Refactor the V0 Selenium tests into a reusable automation framework.

The goal is to demonstrate common QA Automation / SDET concepts that frequently appear in job descriptions:

- Page Object Model
- reusable driver management
- explicit waits
- TestNG lifecycle
- DataProvider
- test groups
- reusable utilities
- failure screenshots

Do not add API, mobile, database, or CI functionality yet.

---

## Starting Point

Assume V0 already contains working Selenium tests against:

`https://www.saucedemo.com/`

V1 should preserve existing test behavior while improving architecture.

---

## Target Structure

Refactor toward:

```text
ecommerce-qa-automation/
│
├── pom.xml
├── testng.xml
├── README.md
├── src/
│   ├── main/
│   │   └── java/
│   │       ├── config/
│   │       │   └── ConfigManager.java
│   │       ├── driver/
│   │       │   └── WebDriverFactory.java
│   │       ├── pages/
│   │       │   ├── LoginPage.java
│   │       │   ├── ProductsPage.java
│   │       │   ├── CartPage.java
│   │       │   └── CheckoutPage.java
│   │       └── utils/
│   │           ├── WaitUtils.java
│   │           └── ScreenshotUtils.java
│   └── test/
│       └── java/
│           ├── base/
│           │   └── BaseTest.java
│           ├── listeners/
│           │   └── TestListener.java
│           ├── data/
│           │   └── TestDataProvider.java
│           └── tests/
│               ├── LoginTest.java
│               ├── ProductTest.java
│               ├── CartTest.java
│               └── CheckoutTest.java
```

---

## Page Object Model

Each page object should own:

- selectors;
- page-specific actions;
- page-specific state queries.

Example:

```java
public class LoginPage {
    public void login(String username, String password) { ... }
    public boolean isErrorDisplayed() { ... }
}
```

Tests should express business behavior:

```java
loginPage.login(username, password);
assertTrue(productsPage.isLoaded());
```

Avoid exposing Selenium locator details directly inside tests.

---

## WebDriver Factory

Create `WebDriverFactory`.

V1 may support Chrome only, but the factory should make future cross-browser support straightforward.

Example API:

```java
WebDriver createDriver(String browser)
```

Do not implement Firefox yet unless trivial.

---

## Explicit Waits

Create reusable explicit-wait utilities.

Use:

- `WebDriverWait`
- `ExpectedConditions`

Avoid:

```java
Thread.sleep(...)
```

Support common actions such as:

```text
wait until visible
wait until clickable
wait until URL contains
```

Do not over-engineer a generic wrapper around every Selenium operation.

---

## TestNG Features

V1 should demonstrate:

### Lifecycle

- `@BeforeMethod`
- `@AfterMethod`

### Data-Driven Testing

Use `@DataProvider`.

Example login matrix:

```text
valid user / valid password
locked user / valid password
valid user / invalid password
empty user / valid password
```

### Groups

Use groups such as:

```text
smoke
regression
negative
```

Allow commands such as:

```bash
mvn test -Dgroups=smoke
```

if practical.

---

## Failure Screenshots

Add a TestNG listener.

On test failure:

```text
test fails
→ capture screenshot
→ save under test-output/screenshots/
```

Use a deterministic filename containing:

- test name;
- timestamp or unique suffix.

Do not take screenshots for successful tests unless debug mode is enabled.

---

## Test Count

Expand toward approximately:

```text
20–25 web UI tests
```

Coverage should include:

### Authentication
- success
- invalid password
- locked user
- empty fields

### Products
- load page
- sort A-Z
- sort Z-A
- sort price low-high
- sort price high-low

### Cart
- add item
- add multiple items
- remove item
- cart badge validation
- cart contents validation

### Checkout
- successful checkout
- missing first name
- missing last name
- missing postal code
- item total / displayed total checks where stable

---

## Selector Strategy

Prefer:

1. stable IDs
2. stable CSS selectors
3. semantic attributes
4. XPath only when necessary

Document any intentionally chosen XPath.

Avoid brittle selectors based on visual DOM position.

---

## README Upgrade

Document:

- framework structure;
- Page Object Model;
- TestNG groups;
- DataProvider;
- screenshot behavior;
- explicit-wait strategy.

---

## Non-Goals

Do NOT implement:

- REST Assured
- Appium
- SQL
- Docker database
- GitHub Actions
- Allure
- Jenkins
- Playwright

---

## Acceptance Criteria

V1 is complete when:

- [ ] V0 test behavior still works.
- [ ] Tests use Page Objects.
- [ ] Selenium selectors are removed from test classes.
- [ ] Explicit waits replace hard-coded sleeps.
- [ ] DataProvider is used for at least one suite.
- [ ] Smoke/regression/negative groups exist.
- [ ] Failure screenshots are automatically captured.
- [ ] Approximately 20–25 web tests exist.
- [ ] `mvn test` still runs successfully.
- [ ] README explains framework architecture.

---

## Codex Instructions

1. Refactor V0 rather than rewriting every test unnecessarily.
2. Keep page objects readable.
3. Do not create a giant `BasePage` abstraction unless clearly useful.
4. Keep TestNG lifecycle in `BaseTest`.
5. Do not add API/mobile/database dependencies.
6. Use explicit waits, not sleeps.
7. Stop after V1 acceptance criteria pass.

At the end, summarize:

- page objects created;
- test count;
- groups;
- data-driven scenarios;
- screenshot behavior.
