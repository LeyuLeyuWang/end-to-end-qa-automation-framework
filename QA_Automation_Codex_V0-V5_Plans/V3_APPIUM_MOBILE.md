# QA Automation Project V3 — Mobile Automation with Appium

## Purpose

Add Android mobile UI automation to the project.

The goal is to demonstrate:

- Appium
- Android automation
- native element locators
- mobile gestures
- reusable mobile page objects
- driver lifecycle
- mobile test design

Keep the mobile suite smaller than the Selenium Web suite.

Do not build a mobile application.

---

## Demo App

Use a public Android demo application intended for test automation.

Preferred option:

**Sauce Labs My Demo App**

Use the official/public APK source available from Sauce Labs at implementation time.

If the current package/app path changes, document the exact version used in README.

Do not download untrusted APKs.

---

## Tooling

Use:

- Appium 2.x
- Java Appium Client
- Android SDK
- Android Emulator
- TestNG
- Maven

Use UiAutomator2 for Android.

Do not add iOS automation in V3.

---

## Repository Changes

Add:

```text
src/main/java/
├── mobile/
│   ├── driver/
│   │   └── AppiumDriverFactory.java
│   └── pages/
│       ├── MobileLoginPage.java
│       ├── MobileProductsPage.java
│       └── MobileCartPage.java

src/test/java/
└── mobile/
    ├── MobileLoginTest.java
    ├── MobileProductTest.java
    └── MobileCartTest.java
```

Keep Web and Mobile page objects separate.

---

## Driver Configuration

Centralize:

- Appium server URL;
- platform name;
- automation name;
- device/emulator name;
- app path;
- optional app package/activity.

Use configuration rather than hard-coding these in tests.

Example capability concepts:

```text
platformName = Android
automationName = UiAutomator2
deviceName = ...
app = ...
```

---

## Required Mobile Scenarios

Target approximately 8–12 tests.

### App Launch

- app launches;
- expected landing/product screen appears.

### Authentication

If supported by the chosen demo app:

- valid login;
- invalid login.

### Product

- open product;
- scroll product list;
- verify product detail.

### Cart

- add product;
- verify cart count;
- remove product if supported.

### Mobile Interaction

Demonstrate at least:

- tap;
- text input;
- swipe or scroll;
- mobile-specific locator;
- app reset or relaunch.

Optional:

- background and foreground app.

---

## Locator Strategy

Prefer stable mobile locators:

1. accessibility ID
2. resource ID
3. platform-supported stable selector
4. XPath only when necessary

Avoid fragile full XPath trees.

---

## Mobile Page Objects

Tests should not contain large amounts of raw Appium locator code.

Example:

```java
mobileProductsPage.openProduct("Sauce Labs Backpack");
mobileProductsPage.addToCart();
assertEquals(mobileCartPage.getItemCount(), 1);
```

---

## Environment Documentation

README must explain:

- install Appium;
- install Android SDK;
- create/start emulator;
- install UiAutomator2 driver;
- configure APK path;
- start Appium server;
- run mobile tests.

Example commands may include:

```bash
appium driver install uiautomator2
appium
mvn test -Dgroups=mobile
```

Use actual working commands for the chosen environment.

---

## Error Handling

Provide useful errors for:

- Appium server unavailable;
- emulator unavailable;
- app path missing;
- driver session creation failure.

Do not silently skip environment problems.

---

## Testing Scope

Mobile tests do not need to duplicate all Web scenarios.

The purpose is to demonstrate cross-platform automation, not maximize test count.

---

## Non-Goals

Do NOT implement:

- iOS
- BrowserStack/Sauce cloud device farms
- mobile performance testing
- SQL
- Docker database
- GitHub Actions mobile emulator execution unless it is trivial and stable
- Playwright

---

## Acceptance Criteria

V3 is complete when:

- [ ] Existing Web and API tests still work.
- [ ] Appium 2.x is configured.
- [ ] Android emulator can create a test session.
- [ ] Public demo APK launches.
- [ ] Reusable mobile page objects exist.
- [ ] Approximately 8–12 mobile tests exist.
- [ ] At least one mobile gesture is automated.
- [ ] Mobile locators are stable and readable.
- [ ] README contains reproducible environment setup.

---

## Codex Instructions

1. Do not build a mobile app.
2. Use an official/public demo APK.
3. Keep Android configuration isolated.
4. Keep Appium code separate from Selenium Web code.
5. Prefer accessibility/resource IDs over XPath.
6. Do not add cloud-device infrastructure.
7. Stop once V3 acceptance criteria pass.

At the end, summarize:

- demo app used;
- Android/Appium versions;
- mobile scenarios;
- emulator configuration;
- exact run command.
