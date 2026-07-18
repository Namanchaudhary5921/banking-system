# Multi-Tier Banking System

A 3-tier banking application built as a portfolio project — separates
**presentation**, **business logic**, and **data** into distinct layers,
the way a real financial-services system is structured, with the SDLC
artifacts (requirements, design, test plan) to show how it was planned
and verified, not just coded.

## Architecture

```
frontend/   →  Presentation tier (static HTML/CSS/JS, calls REST API)
backend/    →  Business + a thin data-access tier (Spring Boot, Java, JPA)
db/         →  Canonical PostgreSQL schema, stored procedures, triggers, views
docs/       →  SDLC docs: requirements.md, design.md, test-plan.md
```

See `docs/design.md` for the full architecture diagram and design rationale.

## Features

- Customer onboarding with KYC-style uniqueness checks (email, national ID)
- Account management (CHECKING / SAVINGS, auto-generated account numbers)
- Deposits, withdrawals, and **atomic** transfers between accounts
  - Transfers use row-level pessimistic locking + `@Transactional` rollback,
    so a failure mid-transfer never leaves one account debited without the
    other credited
- Rule-based fraud/risk engine: flags large transactions, high transaction
  velocity, and withdrawals over 90% of balance
- Full audit trail of every state-changing action
- Role-based auth (ADMIN / TELLER) via Spring Security
- Reporting endpoints (AUM summary, flagged transactions, audit log)

## Tech Stack

- **Backend:** Java 17, Spring Boot 3, Spring Data JPA, Spring Security, Maven
- **Database:** H2 (in-memory, for zero-setup local demo) / PostgreSQL (production schema in `db/`)
- **Frontend:** Plain HTML, CSS, JavaScript (no build step required)
- **Testing:** JUnit 5, Mockito

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- (Optional) PostgreSQL 14+ if you want to run against the production schema in `db/`

### 1. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. The H2 console is available at
`http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:bankingdb`).

Demo credentials (HTTP Basic Auth, defined in `SecurityConfig.java`):

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| teller | teller123 | TELLER |

### 2. Open the frontend

Just open `frontend/index.html` directly in a browser (or serve it with
any static file server, e.g. `npx serve frontend`). Enter the demo
credentials in the top bar, then use the tabs to onboard customers, open
accounts, and run transactions.

### 3. Run the tests

```bash
cd backend
mvn test
```

### 4. (Optional) Run against PostgreSQL

1. Create a database and run `db/schema.sql`, then `db/stored-procedures.sql`, then optionally `db/seed-data.sql`.
2. In `backend/src/main/resources/application.properties`, comment out the H2 block and uncomment the PostgreSQL block.
3. `mvn spring-boot:run`

## Project Structure

```
banking-system/
├── README.md
├── CHANGELOG.md
├── docs/
│   ├── requirements.md
│   ├── design.md
│   └── test-plan.md
├── db/
│   ├── schema.sql
│   ├── stored-procedures.sql
│   └── seed-data.sql
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wellsfargo/bankingsystem/
│       │   ├── BankingSystemApplication.java
│       │   ├── config/          (security, CORS)
│       │   ├── model/           (JPA entities)
│       │   ├── repository/      (Spring Data repositories)
│       │   ├── dto/             (request/response objects)
│       │   ├── service/         (business logic — the core of the app)
│       │   ├── controller/      (REST endpoints)
│       │   └── exception/       (custom exceptions + global handler)
│       ├── main/resources/      (application.properties, data.sql)
│       └── test/java/.../service/  (unit tests)
└── frontend/
    ├── index.html
    ├── css/style.css
    └── js/
        ├── api.js   (REST client)
        └── app.js   (UI logic)
```

## Notes on Scope

This is a portfolio/demo project, not a production banking system. Auth
uses in-memory demo users, there's no real KYC/payment-rail integration,
and the fraud engine is intentionally simple and explainable rather than
ML-based. See `docs/requirements.md` for the full in/out-of-scope list.

## FINAL LOOK
<img width="1436" height="812" alt="Screenshot 2026-07-18 at 3 51 49 PM" src="https://github.com/user-attachments/assets/edfa8137-4c4c-4d21-9780-e6795c7f68d7" />
<img width="1438" height="815" alt="Screenshot 2026-07-18 at 3 52 10 PM" src="https://github.com/user-attachments/assets/19d5b416-d2cb-42c4-a3db-dd10c833989b" />
<img width="1440" height="813" alt="Screenshot 2026-07-18 at 3 52 18 PM" src="https://github.com/user-attachments/assets/16a15a74-bb4d-4661-90e7-1192db24e9c2" />
<img width="1440" height="814" alt="Screenshot 2026-07-18 at 3 52 31 PM" src="https://github.com/user-attachments/assets/378a7c77-765d-4687-b7b9-2df5879548be" />
<img width="1440" height="812" alt="Screenshot 2026-07-18 at 3 52 44 PM" src="https://github.com/user-attachments/assets/27c89d95-88c4-4e5e-961c-43c9bea7abef" />
<img width="1440" height="812" alt="Screenshot 2026-07-18 at 3 52 53 PM" src="https://github.com/user-attachments/assets/180b5566-c2ab-46ba-be1d-c16f338fead5" />
<img width="1440" height="814" alt="Screenshot 2026-07-18 at 3 53 17 PM" src="https://github.com/user-attachments/assets/1c9cd105-73f5-496c-94e4-82e0bd578693" />
