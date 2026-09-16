# Cross-Platform QA Automation Framework — Codex Stage Order

Use these implementation plans sequentially.

## Current project decision

The developer approved deferring V3 (Appium) while targeting general Web/API QA roles.
The active route is **V0 → V1 → V2 → V4 → V5**. The original V3 plan remains available
for optional future mobile work. Until it is implemented, V4/V5 mobile prerequisites
and acceptance items do not apply; project documentation must not claim mobile coverage.
Only the current stage is implemented per iteration. Code teaching happens in a separate
chat; development handoffs should focus on implementation, validation, and limitations.

## Project Goal

Build a portfolio-ready QA/Test Automation project focused on skills commonly seen in QA Engineer, Test Engineer, and QA Automation job descriptions.

Primary stack:

**Java · Selenium WebDriver · Appium · TestNG · REST Assured · SQL/JDBC · Maven · Docker · GitHub Actions · Allure**

The project does **not** require building an e-commerce application.

Public systems under test are used instead.

---

## Stage Order

1. `V0_SELENIUM_BASELINE.md`
   - SauceDemo
   - Java
   - Selenium
   - TestNG
   - Maven
   - 8–10 basic Web tests

2. `V1_FRAMEWORK_POM_TESTNG.md`
   - Page Object Model
   - explicit waits
   - DataProvider
   - test groups
   - failure screenshots
   - expand to ~20–25 Web tests

3. `V2_API_REST_ASSURED.md`
   - REST Assured
   - public CRUD API
   - authentication
   - positive/negative API testing

4. `V3_APPIUM_MOBILE.md`
   - Appium 2.x
   - Android Emulator
   - public demo APK
   - 8–12 mobile tests

5. `V4_SQL_DOCKER_DATABASE.md`
   - PostgreSQL
   - Docker Compose
   - JDBC
   - SQL validation

6. `V5_CI_REPORTING_CROSS_BROWSER.md`
   - Chrome + Firefox
   - headless mode
   - safe parallel execution
   - GitHub Actions
   - Allure
   - final README and portfolio polish

---

## Critical Rule for Codex

Implement **only the current stage**.

Do not prematurely add technologies from future stages.

After each stage:

1. run the relevant tests;
2. fix failures;
3. verify acceptance criteria;
4. summarize changes;
5. stop.

The developer should review and understand the current stage before moving to the next one.

---

## Architecture at Completion

```text
                   Cross-Platform QA Automation
                              │
         ┌────────────────────┼────────────────────┐
         ↓                    ↓                    ↓
 Selenium Web UI        REST Assured API         Appium
         ↓                    ↓                    ↓
    SauceDemo           Public Demo API       Android Demo App

                              │
                              ↓
                         TestNG / Maven
                              │
                  ┌───────────┴───────────┐
                  ↓                       ↓
          PostgreSQL / JDBC         GitHub Actions
                  ↓                       ↓
               Docker                Allure Report
```

---

## Portfolio Principle

Do not optimize for the maximum number of technologies.

Optimize for:

1. technologies that appear often in QA/Test job descriptions;
2. real working code;
3. clean architecture;
4. ability to explain every major design choice in an interview;
5. reproducible tests.

Do not fabricate metrics, coverage, bug reductions, or performance improvements.
