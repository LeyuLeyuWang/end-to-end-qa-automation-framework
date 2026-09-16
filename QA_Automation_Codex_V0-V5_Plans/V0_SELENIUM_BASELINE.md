# QA Automation Project V0 — Selenium Baseline

## Project Goal

Build the smallest working QA automation project for a public e-commerce demo site.

The purpose of V0 is to learn and demonstrate the core Selenium workflow:

> open browser → locate element → interact → assert result

Do not build a large framework yet.

The project under test is **SauceDemo**:

`https://www.saucedemo.com/`

The automation project itself is the portfolio project. Do not build an e-commerce application.

---

## Tech Stack

Use:

- Java 17+
- Selenium WebDriver
- TestNG
- Maven

Do not add Appium, REST Assured, SQL, Docker, Allure, Jenkins, or Playwright in V0.

---

## Repository Structure

Start small:

```text
ecommerce-qa-automation/
│
├── pom.xml
├── testng.xml
├── README.md
└── src/
    └── test/
        └── java/
            └── tests/
                ├── LoginTest.java
                ├── ProductTest.java
                ├── CartTest.java
                └── CheckoutTest.java
```

---

## Required Test Scenarios

Implement approximately 8–10 tests.

### Login

1. Successful login with standard user.
2. Invalid password.
3. Empty username.
4. Locked-out user.

### Product

5. Product page loads after login.
6. Product sort option changes ordering.

### Cart

7. Add one product to cart.
8. Remove a product from cart.

### Checkout

9. Successful checkout.
10. Missing required checkout information shows an error.

---

## Selenium Requirements

Demonstrate:

- `WebDriver`
- `ChromeDriver`
- `By.id`
- `By.cssSelector`
- `By.xpath` only where justified
- `sendKeys`
- `click`
- `getText`
- `getCurrentUrl`
- assertions with TestNG

Avoid unnecessary abstraction.

It is acceptable for test code to contain some repeated Selenium calls in V0. V1 will refactor them.

---

## Driver Lifecycle

Use TestNG lifecycle methods:

```java
@BeforeMethod
@AfterMethod
```

Each test should start from a predictable browser state.

Example behavior:

```text
@BeforeMethod
→ create driver
→ open SauceDemo

@Test
→ execute scenario

@AfterMethod
→ quit browser
```

---

## Assertions

Tests must verify outcomes.

Do not write automation that only clicks through the UI.

Examples:

```text
Login succeeded
→ assert URL contains "inventory"

Cart add
→ assert cart badge = "1"

Checkout
→ assert confirmation message is displayed
```

---

## Configuration

Put basic constants in one place.

At minimum:

```text
BASE_URL
VALID_USERNAME
VALID_PASSWORD
```

Do not commit secrets. SauceDemo public credentials are test credentials, but still keep configuration tidy.

---

## README Requirements

README should explain:

- what is being tested;
- tech stack;
- how to install dependencies;
- how to run tests;
- which scenarios are covered.

Example command:

```bash
mvn test
```

---

## Non-Goals

Do NOT implement:

- Page Object Model
- custom wait utilities
- DataProvider
- Appium
- API testing
- database testing
- CI/CD
- Allure
- Docker
- cross-browser execution
- parallel execution

---

## Acceptance Criteria

V0 is complete when:

- [ ] Maven project builds successfully.
- [ ] Selenium launches Chrome.
- [ ] SauceDemo opens successfully.
- [ ] 8–10 tests exist.
- [ ] Tests contain real assertions.
- [ ] Driver cleanup happens after each test.
- [ ] `mvn test` runs the suite.
- [ ] README explains how to run the project.

---

## Codex Instructions

1. Keep V0 intentionally simple.
2. Do not introduce Page Object Model yet.
3. Do not add future-stage dependencies.
4. Prefer stable selectors such as IDs and test-specific attributes.
5. Avoid `Thread.sleep`.
6. If waiting is required, use a minimal explicit wait locally.
7. Stop after V0 acceptance criteria pass.

At the end, summarize:

- files created;
- tests implemented;
- command to run tests;
- any SauceDemo behavior assumptions.
