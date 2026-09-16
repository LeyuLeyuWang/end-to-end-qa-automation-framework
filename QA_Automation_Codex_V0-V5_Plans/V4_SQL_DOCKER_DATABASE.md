# QA Automation Project V4 — Database Validation with SQL, JDBC, and Docker

## Purpose

Add database testing skills without pretending the public SauceDemo website exposes its database.

The goal is to demonstrate:

- SQL
- JDBC
- test data setup
- backend data validation
- Dockerized database dependency

This database test layer is a separate controlled test fixture.

Do not claim it is SauceDemo's real backend.

---

## Database

Use PostgreSQL in Docker.

The project should provide:

```text
docker-compose.yml
```

or equivalent modern Compose configuration.

---

## Schema

Keep the schema small.

Suggested tables:

```sql
users
orders
order_items
```

Example:

```text
users
- id
- email
- status

orders
- id
- user_id
- status
- total_amount
- created_at
```

Create seed data automatically.

---

## Repository Changes

Add:

```text
docker/
├── docker-compose.yml
└── init/
    └── init.sql

src/main/java/
└── database/
    ├── DatabaseClient.java
    └── DatabaseConfig.java

src/test/java/
└── database/
    └── DatabaseValidationTest.java
```

---

## JDBC Layer

Create a small JDBC client.

Responsibilities:

- open connection;
- execute parameterized queries;
- return typed/usable results;
- close resources safely.

Use prepared statements.

Avoid string-concatenated SQL with test input.

---

## Required SQL Coverage

Demonstrate:

```text
SELECT
INSERT
UPDATE
DELETE
JOIN
```

Not every operation needs a separate portfolio bullet, but tests should exercise common SQL validation patterns.

---

## Suggested Test Scenarios

Approximately 5–8 tests.

Examples:

### User Validation

- seeded user exists;
- user status matches expected value.

### Order Validation

- insert order;
- query order;
- update order status;
- validate amount;
- join order with user;
- delete test order;
- verify deletion.

---

## Test Isolation

Each test must avoid corrupting shared state.

Use one of:

- setup/cleanup;
- transactions with rollback;
- unique IDs.

Prefer deterministic tests.

---

## Docker Workflow

Expected workflow:

```text
docker compose up -d
      ↓
PostgreSQL starts
      ↓
schema + seed data initialized
      ↓
mvn test -Dgroups=database
      ↓
database assertions
```

README must include exact commands.

---

## Configuration

Use environment variables or Maven properties for:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
```

Provide `.env.example` if needed.

Do not commit real credentials.

Local test credentials may be documented safely if clearly scoped to Docker development.

---

## README Positioning

Be explicit:

> The PostgreSQL module is a local test fixture used to demonstrate database validation with JDBC and SQL. It is not connected to SauceDemo's private backend.

This avoids misleading portfolio claims.

---

## Non-Goals

Do NOT implement:

- a real application backend
- ORM frameworks unless necessary
- distributed databases
- database performance benchmarking
- Redis
- Kafka
- production secrets management
- Playwright

---

## Acceptance Criteria

V4 is complete when:

- [ ] Existing Web/API/Mobile tests still work.
- [ ] PostgreSQL starts using Docker Compose.
- [ ] Schema and seed data initialize automatically.
- [ ] JDBC connection works.
- [ ] SQL validation tests exist.
- [ ] Prepared statements are used.
- [ ] JOIN validation is demonstrated.
- [ ] Tests clean up their state.
- [ ] README clearly states database scope.
- [ ] Approximately 5–8 DB tests exist.

---

## Codex Instructions

1. Keep the database module small.
2. Do not fake a connection between PostgreSQL and SauceDemo.
3. Use JDBC directly to demonstrate SQL knowledge.
4. Use prepared statements.
5. Keep test data deterministic.
6. Do not add unrelated backend services.
7. Stop after V4 acceptance criteria pass.

At the end, summarize:

- schema;
- test scenarios;
- Docker commands;
- JDBC configuration;
- cleanup strategy.
