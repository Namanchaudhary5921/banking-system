# Requirements — Multi-Tier Banking System

## 1. Purpose
A portfolio project demonstrating a 3-tier banking application: presentation
(static HTML/JS), business logic (Spring Boot services), and data (JPA/SQL),
with SDLC artifacts to show how the project was planned and tested, not just
coded.

## 2. Stakeholders
- **Customer** — owns accounts, initiates deposits/withdrawals/transfers
- **Teller/Admin** — onboards customers, opens accounts, views reports
- **Risk/Compliance (implied)** — consumes the fraud-flag and audit-log reports

## 3. Functional Requirements

| ID | Requirement |
|----|-------------|
| FR-1 | System shall allow onboarding a new customer with unique email and national ID |
| FR-2 | System shall allow opening a CHECKING or SAVINGS account for an existing customer |
| FR-3 | System shall allow deposits, which increase account balance |
| FR-4 | System shall allow withdrawals, which decrease balance only if sufficient funds exist |
| FR-5 | System shall allow transfers between two accounts as a single atomic operation |
| FR-6 | System shall reject a withdrawal/transfer that would overdraw an account |
| FR-7 | System shall evaluate every transaction against fraud rules (large amount, velocity, high % of balance) and flag matches |
| FR-8 | System shall record an audit log entry for every state-changing action |
| FR-9 | System shall expose a summary report (customers, accounts, AUM, flagged count) |
| FR-10 | System shall restrict account/transaction endpoints to authenticated ADMIN/TELLER roles |

## 4. Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-1 | Transfers must be atomic — a failure mid-transfer must leave both accounts unchanged |
| NFR-2 | Concurrent requests against the same account must not corrupt the balance (row-level locking) |
| NFR-3 | Monetary values must use fixed-point decimal types (no floating point) |
| NFR-4 | API responses for errors must return structured JSON with a clear message |
| NFR-5 | Frontend must be a separate static app that only talks to the backend over REST |

## 5. Out of Scope (for this portfolio version)
- Real KYC/identity verification integrations
- Real payment rails (ACH/SWIFT)
- Production-grade secrets management (demo uses in-memory users)
- Horizontal scaling / multi-node deployment
