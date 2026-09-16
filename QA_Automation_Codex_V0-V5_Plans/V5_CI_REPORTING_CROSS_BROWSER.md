# QA Automation Project V5 — CI/CD, Reporting, Cross-Browser, and Portfolio Polish

## Purpose

Turn the V4 project into a portfolio-ready QA Automation framework.

The goal is to demonstrate:

- GitHub Actions
- automated regression execution
- cross-browser testing
- parallel execution
- Allure reporting
- test artifacts
- clean project documentation

Do not add another major testing technology.

---

## Starting Point

Assume the project already includes:

- Selenium Web UI tests
- TestNG
- Page Object Model
- REST Assured API tests
- Appium Android tests
- SQL/JDBC validation
- Dockerized PostgreSQL

---

## Part A — Cross-Browser Selenium

Upgrade `WebDriverFactory` to support:

```text
Chrome
Firefox
```

Browser should be selectable via Maven/System property.

Example:

```bash
mvn test -Dbrowser=chrome
mvn test -Dbrowser=firefox
```

Do not duplicate test classes per browser.

---

## Part B — Headless CI Execution

Web tests should support headless execution.

Example configuration:

```text
HEADLESS=true
```

Local default may remain headed if preferred.

CI should use headless mode.

---

## Part C — Parallel Execution

Use TestNG parallel execution carefully.

Suitable candidates:

- Web test classes;
- API tests.

Do not parallelize tests that share mutable state without isolation.

If WebDriver becomes parallel, use thread-safe driver management such as `ThreadLocal<WebDriver>`.

Keep implementation understandable.

---

## Part D — Allure Reporting

Add Allure TestNG integration.

Capture:

- passed/failed tests;
- failure stack traces;
- screenshots;
- relevant test metadata.

On Selenium failure:

```text
failure
→ screenshot
→ attach to Allure
```

Optional:

- attach REST request/response on API failure.

Do not overload reports with excessive logs.

---

## Part E — GitHub Actions

Create:

```text
.github/workflows/qa-tests.yml
```

Recommended workflow:

```text
push / pull_request
      ↓
setup Java
      ↓
restore Maven cache
      ↓
start Docker PostgreSQL
      ↓
run API tests
      ↓
run DB tests
      ↓
run Selenium headless tests
      ↓
generate Allure results
      ↓
upload artifacts
```

Mobile/Appium CI is optional because Android emulator setup can make CI slow and brittle.

If mobile CI is not included, document:

> Mobile tests run locally with Android Emulator and Appium.

Do not make the entire pipeline fail because Appium infrastructure is unavailable unless mobile CI has been deliberately implemented.

---

## Part F — Test Suite Organization

Use TestNG groups:

```text
smoke
regression
web
api
mobile
database
```

Support useful commands.

Examples:

```bash
mvn test -Dgroups=smoke
mvn test -Dgroups=web
mvn test -Dgroups=api
mvn test -Dgroups=database
```

Actual Maven configuration must match documented commands.

---

## Part G — README Finalization

README should include:

### Project Summary

Example:

> A cross-platform QA automation framework covering Web UI, REST API, Android mobile, and database validation.

### Tech Stack

```text
Java
Selenium WebDriver
Appium
TestNG
REST Assured
SQL/JDBC
Maven
PostgreSQL
Docker
GitHub Actions
Allure
```

### Architecture

```text
                    QA Automation Framework
                           │
        ┌──────────────────┼───────────────────┐
        ↓                  ↓                   ↓
   Selenium Web       REST Assured          Appium
        ↓                  ↓                   ↓
   SauceDemo          Public REST API     Android Demo App
        │
        └──────────────┐
                       ↓
                 TestNG / Maven
                       ↓
                GitHub Actions
                       ↓
                Allure Reports

Database validation:
PostgreSQL + JDBC + Docker
```

### How to Run

Document commands for:

- all local tests;
- Web only;
- API only;
- mobile only;
- DB only;
- smoke;
- browser selection.

### Screenshots

Add real screenshots later for:

- Allure dashboard;
- Selenium execution;
- Appium emulator;
- GitHub Actions successful run.

Do not fabricate screenshots.

---

## Part H — Project Cleanup

Before V5 is complete:

- remove dead code;
- remove unused dependencies;
- centralize versions in `pom.xml`;
- verify no secrets;
- verify `.gitignore`;
- ensure screenshots/test artifacts are ignored when appropriate;
- clean naming;
- add type-safe/configurable helpers where useful;
- ensure failures have useful messages.

---

## Portfolio Metrics

Only state factual metrics.

Safe examples:

```text
Implemented 20+ Web UI automated tests.
Implemented 10+ REST API tests.
Implemented 8+ Android Appium tests.
```

Only use counts that actually exist.

Do not invent:

- "% faster releases"
- "% fewer bugs"
- "X% coverage"

unless truly measured.

---

## Optional Jenkinsfile

A simple `Jenkinsfile` may be added only if it takes very little time and does not distract from GitHub Actions.

GitHub Actions is the primary CI implementation.

Do not spend significant time on Jenkins for this project.

---

## Non-Goals

Do NOT add:

- Playwright
- Cypress
- Kafka
- Kubernetes
- cloud device farms
- load testing
- security testing platform
- custom Selenium Grid unless specifically needed
- production deployment

V5 is about polish, automation, and explainability.

---

## Acceptance Criteria

V5 is complete when:

- [ ] Chrome execution works.
- [ ] Firefox execution works.
- [ ] Headless Web execution works.
- [ ] TestNG grouping works.
- [ ] Safe parallel execution works where enabled.
- [ ] Allure results are generated.
- [ ] Failure screenshots appear in reporting.
- [ ] GitHub Actions runs Web/API/DB tests automatically.
- [ ] CI uploads useful artifacts.
- [ ] Mobile test instructions are reproducible.
- [ ] README accurately explains architecture and commands.
- [ ] No fabricated metrics or claims appear.
- [ ] Repository is clean enough for a recruiter/interviewer to inspect.

---

## Codex Instructions

1. Do not add another test framework.
2. Use Selenium as the Web automation framework.
3. Keep CI reliable rather than overly ambitious.
4. Mobile CI is optional.
5. Prefer GitHub Actions over Jenkins.
6. Use Allure for reporting.
7. Preserve clear separation between Web/API/Mobile/DB modules.
8. Stop after V5 acceptance criteria pass.

At the end, provide:

- final test counts by category;
- supported browsers;
- CI workflow summary;
- test commands;
- remaining environment limitations.
