# QA Automation Project V2 — REST API Testing with REST Assured

## Purpose

Add an API automation layer to the existing Selenium framework.

The goal is to demonstrate:

- REST API testing
- HTTP methods
- authentication
- JSON validation
- positive and negative test cases
- request/response models
- reusable API client design

Use Java + REST Assured.

Do not add Appium, SQL, Docker database, or CI/CD yet.

---

## Public API Under Test

Use a stable public test API such as:

**Restful Booker**

`https://restful-booker.herokuapp.com/`

If that service is unavailable at implementation time, use another public CRUD testing API with equivalent functionality and document the substitution.

Do not build an API server.

---

## Repository Changes

Add:

```text
src/main/java/
├── api/
│   ├── BookingClient.java
│   ├── AuthClient.java
│   └── models/
│       ├── Booking.java
│       ├── BookingDates.java
│       └── AuthRequest.java

src/test/java/
└── api/
    ├── BookingApiTest.java
    └── AuthApiTest.java
```

---

## Required HTTP Coverage

Implement tests for:

```text
GET
POST
PUT or PATCH
DELETE
```

Cover:

- successful requests;
- invalid input;
- invalid/nonexistent resource;
- unauthorized modification;
- response structure;
- content type;
- status codes.

---

## Suggested Test Scenarios

Approximately 10–15 API tests.

### Authentication

- valid credentials return token;
- invalid credentials do not produce valid authentication.

### Booking

- create booking;
- fetch created booking;
- update booking;
- partially update booking if supported;
- delete booking;
- fetch nonexistent booking;
- invalid payload;
- verify field values;
- verify response content type;
- verify expected status codes.

---

## REST Assured Style

Use readable REST Assured syntax.

Example:

```java
given()
    .contentType(ContentType.JSON)
    .body(request)
.when()
    .post("/booking")
.then()
    .statusCode(200);
```

Avoid duplicating base URL and common headers in every test.

---

## API Client Abstraction

Create small reusable clients.

Example:

```java
public class BookingClient {
    public Response createBooking(Booking booking) { ... }
    public Response getBooking(int id) { ... }
    public Response updateBooking(int id, Booking booking, String token) { ... }
    public Response deleteBooking(int id, String token) { ... }
}
```

Do not bury assertions inside the API client.

Tests should own assertions.

---

## Request / Response Models

Use POJOs for request bodies where reasonable.

Avoid constructing every JSON body as raw strings.

If JSON serialization is needed, use Jackson or an equivalent lightweight library.

---

## Test Data

Do not hard-code the same exact test user/data everywhere.

Use:

- TestNG DataProvider where helpful;
- a small test data factory;
- unique values where API state requires it.

---

## Assertions

Validate:

- status code;
- content type;
- JSON fields;
- created/updated values;
- authentication behavior.

Optional:

- basic response-time threshold, but do not treat a public demo service as a performance benchmark.

---

## Logging

Enable useful request/response logging on failure.

Avoid printing sensitive tokens unnecessarily.

---

## README Upgrade

Add a section for:

- API testing architecture;
- public API used;
- REST Assured;
- supported request types;
- how to run API tests only.

Example:

```bash
mvn test -Dgroups=api
```

if groups are configured.

---

## Non-Goals

Do NOT implement:

- performance/load testing
- JMeter
- contract testing platform
- Appium
- SQL validation
- Docker database
- CI/CD
- Playwright

---

## Acceptance Criteria

V2 is complete when:

- [ ] Existing Selenium tests still work.
- [ ] REST Assured is configured.
- [ ] Reusable API client classes exist.
- [ ] GET/POST/PUT-or-PATCH/DELETE are covered.
- [ ] Positive and negative API tests exist.
- [ ] Authentication flow is tested.
- [ ] Request bodies use structured Java models where reasonable.
- [ ] API assertions verify response data, not only status codes.
- [ ] Approximately 10–15 API tests exist.
- [ ] README documents API test execution.

---

## Codex Instructions

1. Keep Web and API tests separate.
2. Reuse Maven/TestNG infrastructure.
3. Keep API clients free of test assertions.
4. Do not add mobile/database/CI functionality.
5. Avoid external dependencies beyond what is necessary.
6. Stop after V2 acceptance criteria pass.

At the end, summarize:

- API used;
- endpoints covered;
- test count;
- how authentication is handled;
- command to run API tests.
