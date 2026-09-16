# End-to-End QA Automation Framework

> Jenkins is also supported: see the [pipeline and local setup guide](docs/JENKINS.md). GitHub Actions remains available; the local Jenkins job is manually triggered.

**Jenkins validation (2026-09-16):** the first local build passed all 46 scenarios using headless Chrome, published test results, archived Allure reports and cleaned up its isolated database fixture. [View the actual results screenshot](docs/images/jenkins-test-results.png).

**English** | [简体中文](README.zh-CN.md)

A Java QA automation portfolio covering browser workflows, REST APIs, and database validation. The project combines reusable test components with isolated test data, failure diagnostics, cross-browser execution, and a GitHub Actions workflow.

**46 business test executions · 35 test methods · Chrome & Firefox · Allure reporting**

## What this project tests

| Module | Target | Scenarios | Examples |
|---|---|---:|---|
| Web UI | SauceDemo | 24 | Login validation, product sorting, cart state, checkout and displayed totals |
| REST API | Restful Booker | 14 | Authentication, CRUD, partial updates, filtering and rejected modifications |
| Database | Local PostgreSQL fixture | 8 | Inserts, updates, deletes, joins, totals, foreign keys and parameter binding |

These are **three independent systems**, not a connected frontend/API/database stack. The PostgreSQL fixture is not SauceDemo's or Restful Booker's backend. The Web checkout tests do not verify real payments. Mobile/Appium testing is not implemented.

The 46 executions include DataProvider expansion. Running the same Web scenarios in two browsers does not increase the number of unique business scenarios. Intentional failure probes are excluded from this count.

## Engineering highlights

- **Page Object Model:** selectors and browser actions are separated from business assertions.
- **Explicit waits:** tests wait for observable page conditions instead of fixed sleeps.
- **Web isolation:** a fresh browser session per test, with thread-local driver and page references.
- **API data ownership:** uniquely identified bookings are tracked and cleaned up; cleanup errors remain visible.
- **Database isolation:** each test uses its own JDBC connection and rolls back its transaction.
- **Meaningful assertions:** rejected API modifications must leave data unchanged; PATCH must preserve unspecified fields.
- **Failure evidence:** Web screenshots are captured before teardown and attached to Allure; API failure logs omit authentication bodies and headers.
- **Controlled parallelism:** two-thread, class-level Web execution; API and database tests remain serial within their suites.

## Architecture and stack

```text
Web tests ── Page Objects ── Selenium ────────── SauceDemo
API tests ── API clients ── REST Assured ─────── Restful Booker
DB tests ─── DatabaseClient ── JDBC ──────────── Docker PostgreSQL
                         │
                    TestNG / Maven
                         │
               Local runs / GitHub Actions
                         │
              Surefire / Allure / failure PNGs
```

Java 17 compilation target; local V5 validation used JDK 21. Dependency and plugin versions are pinned in [pom.xml](pom.xml). The project uses Selenium, TestNG, REST Assured, Jackson, pgJDBC, Allure and Maven Wrapper. PostgreSQL is pinned in [Docker Compose](docker/docker-compose.yml).

```text
src/main/java/pages/       Browser interactions and page observations
src/main/java/api/         HTTP clients, models, configuration and diagnostics
src/main/java/database/    JDBC connection and parameterized SQL helpers
src/test/java/tests/       Web business tests
src/test/java/api/         API tests and data lifecycle
src/test/java/database/    Database tests and transaction lifecycle
docker/                   PostgreSQL schema, seed data and Compose service
.github/workflows/        GitHub Actions workflow
docs/                     Version handoffs, snapshots and learning textbook
```

## Quick start

Prerequisites: JDK 17+ with `JAVA_HOME`, Chrome or Firefox for Web tests, and Docker with Linux containers for database tests. Initial runs require network access to download dependencies, drivers and images. Web/API-only runs do not need Docker.

Clone the repository, then run from its root:

```powershell
git clone https://github.com/LeyuLeyuWang/end-to-end-qa-automation-framework.git
cd end-to-end-qa-automation-framework

# Web only: no database required
.\mvnw.cmd '-Dgroups=web' '-Dheadless=true' test

# Full regression: start the database first
docker compose -f docker/docker-compose.yml up -d --wait
.\mvnw.cmd '-Dheadless=true' clean test

# Generate and view Allure reporting
.\mvnw.cmd allure:report
.\mvnw.cmd allure:serve
```

On Linux/macOS, use `sh ./mvnw` instead of `.\mvnw.cmd`. For example:

```bash
sh ./mvnw -Dgroups=web -Dheadless=true test
```

Defaults: Chrome, headed mode, serial execution. Selenium Manager manages drivers; supported browser downloads require network access. Local validation was performed on Windows.

## Test selection and cross-browser execution

```powershell
.\mvnw.cmd '-Dgroups=api' test
.\mvnw.cmd '-Dgroups=database' test
.\mvnw.cmd '-Dgroups=smoke' '-Dheadless=true' test
.\mvnw.cmd '-Dgroups=negative' '-Dheadless=true' test
.\mvnw.cmd '-Dgroups=web' '-Dbrowser=firefox' '-Dheadless=true' test

# Web classes only, using two worker threads
.\mvnw.cmd -Pparallel-web '-Dbrowser=chrome' '-Dheadless=true' test
.\mvnw.cmd -Pparallel-web '-Dbrowser=firefox' '-Dheadless=true' test
```

| Group | Executions |
|---|---:|
| `web` | 24 |
| `api` | 14 |
| `database` | 8 |
| `regression` | 46 |
| `smoke` | 9 |
| `negative` | 18 |

Groups overlap; do not add their counts. Full-suite `smoke` and `negative` include database tests. The `parallel-web` profile selects [testng-web-parallel.xml](testng-web-parallel.xml), which contains only Web classes. Adding `-Dgroups=smoke` to that profile selects only Web smoke tests. Do not enable method/DataProvider parallelism for the API or database modules without revisiting their shared state.

## Configuration and database fixture

| Setting | Default | Override |
|---|---|---|
| Browser | `chrome` | `-Dbrowser=chrome` or `firefox` |
| Headless | `false` | `-Dheadless=true`, otherwise `HEADLESS` environment variable |
| Web URL | `https://www.saucedemo.com/` | `-DbaseUrl=...` |
| Explicit wait | 10 seconds | `-DwaitSeconds=...` |
| API URL | `https://restful-booker.herokuapp.com` | `-DapiBaseUrl=...` |
| Database host / port | `localhost` / `55432` | `DB_HOST` / `DB_PORT` |
| Database name | `qa_fixture` | `DB_NAME` |
| Database user / password | `qa_local` / `qa_local_only` | `DB_USER` / `DB_PASSWORD` |

Java database configuration precedence is system property (`-DDB_*`) → shell environment variable → default. Use shell variables to configure both Compose and Java; Maven properties do not change container port mappings. Copying [.env.example](.env.example) does **not** make Java load a `.env` file automatically. Use only demo/test credentials: Surefire can record test parameters even though specific Allure/API fields are redacted.

The fixture contains `users`, `orders` and `order_items`, seeded with one user, one order and two items totaling 39.98. SQL uses `PreparedStatement`; money uses `BigDecimal`. Tests roll back their own transactions; generated sequence IDs need not be consecutive.

The Compose project is named `qa-automation-v4`, where the database module was introduced. Its port binds only to `127.0.0.1`. Initialization scripts run on an empty data volume; restarting an existing volume does not rerun them or migrate the schema.

```powershell
docker compose -f docker/docker-compose.yml ps
docker compose -f docker/docker-compose.yml down
```

`down` preserves the named volume. Only when intentionally deleting this project's fixture data, use `down -v` before starting again. Connection troubleshooting: check the Docker engine, service health, port and matching `DB_*` settings. Detailed schema and database scenarios are documented in the [V4 reference](docs/V4_README.md) (Chinese).

## Reports and diagnostic probes

| Output | Location |
|---|---|
| Surefire results | `target/surefire-reports/` |
| Allure raw results | `target/allure-results/` |
| Allure HTML | `target/site/allure-maven-plugin/` |
| Web failure screenshots | `test-output/screenshots/` |

Use `clean test` for a fresh set of Allure results, or select a separate results directory. `allure:report` generates HTML; `allure:serve` serves it for interactive viewing. Maven downloads the Allure CLI when first needed; no global Allure installation or AspectJ is required. Maven `clean` does not clear the separate screenshot directory.

Failure probes explicitly validate failure handling and are not part of the business suite:

```powershell
.\mvnw.cmd '-Dtest=ScreenshotProbe' '-Dheadless=true' '-Dallure.results.directory=target/probe-allure-results' '-Dsurefire.reportsDirectory=target/probe-reports' test
```

Expected: **2 intentional failures and 1 pass**, with separate PNG attachments for both failures. A nonzero Maven exit code is expected. `ApiFailureProbe` and `DatabaseRollbackProbe` similarly exercise cleanup after intentional failure; select them explicitly and use separate report directories.

## GitHub Actions and validation

The [QA workflow](.github/workflows/qa-tests.yml) runs on push, pull request and manual dispatch. Its matrices define four jobs: Chrome Web, Firefox Web, API and database. It sets up Java 21 and Maven caching, uses headless Web class parallelism, and starts a healthy PostgreSQL fixture for the database job.

After failures, jobs still attempt to generate Allure reports and upload results, screenshots and database logs. Artifacts are retained for 14 days. Test failures are not hidden by automatic retries or ignored exit codes.

**Recorded local V5 validation (2026-09-14):** 46/46 full-suite executions passed; parallel headless Chrome and Firefox each passed 24/24 Web scenarios; the screenshot probe produced the expected attachments. The workflow also passed local `actionlint` validation. These records are not a claim that every subsequent hosted run passes. See [GitHub Actions](https://github.com/LeyuLeyuWang/end-to-end-qa-automation-framework/actions) for current hosted results.

Public demo services, network availability and upstream UI/API changes can affect results. This project does not claim measured code coverage, performance improvements or comprehensive security testing.

## Learning material and version history

- [Textbook guide](docs/textbook/README.md): 19 chapters on testing principles, framework concepts, Java syntax and project source, primarily in Chinese with English terminology and interview explanations.
- [Read the textbook as Markdown](docs/textbook/QA_AUTOMATION_TEXTBOOK.md), or clone the repository and open `docs/textbook/index.html` locally. GitHub's HTML file view is not a hosted textbook site.
- [V5 handoff](docs/V5_HANDOFF.md), [V4 reference](docs/V4_README.md), [V2 reference](docs/V2_README.md), [V1 reference](docs/V1_README.md) (Chinese).
- Source snapshots: [V0](docs/v0-baseline.zip), [V1](docs/v1-baseline.zip), [V2](docs/v2-baseline.zip), [V4](docs/v4-baseline.zip).
- Development path: **V0 → V1 → V2 → V4 → V5**. V3 mobile testing was intentionally deferred; historical documents describe their respective snapshots.
