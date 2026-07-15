# Design — Multi-Tier Banking System

## 1. Architecture Overview

```
┌─────────────────────────┐
│   Presentation Tier      │   frontend/  (HTML + CSS + vanilla JS)
│   - index.html            │   Talks to backend only via fetch() calls
│   - api.js / app.js        │   to /api/** endpoints (REST + JSON)
└────────────┬─────────────┘
             │ HTTPS / REST (JSON)
┌────────────▼─────────────┐
│   Business Tier            │   backend/.../service/
│   - CustomerService         │   Owns all validation and business rules.
│   - AccountService           │   Every state-changing method is
│   - TransactionService        │   @Transactional so partial failures
│   - FraudDetectionService      │   roll back cleanly.
│   - AuditService                │
└────────────┬─────────────┘
             │ Spring Data JPA
┌────────────▼─────────────┐
│   Data Tier                │   backend/.../model/, repository/
│   - H2 (local demo)         │   db/schema.sql, stored-procedures.sql
│   - PostgreSQL (production)   │   (canonical schema + triggers/views)
└───────────────────────────┘
```

## 2. Entity-Relationship Overview

```
customers 1───* accounts 1───* transactions
                                  │
                                  ▼ (mirrored by trigger)
                              audit_log

app_users (independent — auth only, not linked to customers in this version)
```

- `customers` → `accounts`: one customer can hold multiple accounts (checking + savings).
- `accounts` → `transactions`: every balance-changing event is an immutable transaction row (append-only ledger style, not just a mutable balance field).
- `audit_log`: system-of-record for "who did what when," written by the app layer (AuditService) and, in the PostgreSQL version, also by a DB trigger as a second line of defense.

## 3. Key Design Decisions

**Why an append-only transaction ledger instead of just updating `balance`?**
Mirrors how real banking systems work — the balance is a derived/cached value,
and the transaction log is the source of truth for reconciliation, disputes,
and auditing.

**Why row-level pessimistic locking on transfers?**
Two simultaneous transfers touching the same account could otherwise both
read the same starting balance and both succeed, silently double-spending
funds. Locking rows (and locking in a consistent account-number order to
avoid deadlocks) prevents that.

**Why a rule-based fraud engine instead of ML?**
For a portfolio project, explainability matters more than sophistication.
Every flag has a plain-English reason, which is realistic for how a first
line of defense (before a real ML model or manual review) typically works.

**Why both H2 (app runtime) and PostgreSQL (db/ folder)?**
H2 keeps the project runnable with zero setup (`mvn spring-boot:run`) for
anyone reviewing the repo. `db/schema.sql` and `db/stored-procedures.sql`
are the deliberately-designed production schema — constraints, indexes,
stored procedures, triggers, views — which is what demonstrates SQL depth
independent of whatever ORM the app happens to use.

## 4. API Surface (summary)

| Method | Path | Purpose |
|--------|------|---------|
| POST | /api/customers | Onboard a customer |
| GET | /api/customers | List customers |
| POST | /api/accounts | Open an account |
| GET | /api/accounts | List accounts |
| POST | /api/transactions/deposit | Deposit funds |
| POST | /api/transactions/withdraw | Withdraw funds |
| POST | /api/transactions/transfer | Transfer between accounts |
| GET | /api/transactions/account/{accountNumber} | Transaction history |
| GET | /api/transactions/flagged | Fraud-flagged transactions |
| GET | /api/reports/summary | Dashboard summary stats |
| GET | /api/reports/audit-log | Full audit trail |

## 5. Sequence — Transfer (happy path + failure)

```
Client → POST /api/transactions/transfer
  TransactionService.transfer()
    lock(fromAccount) lock(toAccount)     [pessimistic DB lock]
    if fromAccount.balance < amount:
        throw InsufficientFundsException  → @Transactional rolls back
                                             → 422 returned to client,
                                               NO rows changed
    else:
        debit fromAccount, credit toAccount
        insert TRANSFER_OUT + TRANSFER_IN rows
        write audit log entry
        commit                             → 200 returned to client
```
