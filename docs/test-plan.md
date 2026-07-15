# Test Plan — Multi-Tier Banking System

## 1. Strategy
- **Unit tests** (JUnit 5 + Mockito): isolate service-layer logic from the
  database. See `backend/src/test/java/.../service/`.
- **Manual/integration tests**: exercised via the frontend or a REST client
  (Postman/curl) against the running H2-backed app.
- Future work (not yet implemented): `@SpringBootTest` + Testcontainers
  running against a real PostgreSQL instance to validate `db/schema.sql`
  and the stored procedures directly.

## 2. Unit Test Matrix (implemented)

| Test | Class | Verifies |
|------|-------|----------|
| deposit_increasesBalance | TransactionServiceTest | Balance and transaction record update correctly on deposit |
| withdraw_decreasesBalance_whenFundsSufficient | TransactionServiceTest | Balance decreases correctly on valid withdrawal |
| withdraw_throwsInsufficientFunds_andLeavesBalanceUnchanged | TransactionServiceTest | Overdraft attempts are rejected and balance is untouched (atomicity) |
| openAccount_assignsSavingsInterestRate | AccountServiceTest | Correct interest rate assigned based on account type |

Run with: `mvn test` (from `backend/`)

## 3. Manual Test Scenarios (recommended before a demo/interview walkthrough)

| # | Scenario | Steps | Expected Result |
|---|----------|-------|------------------|
| 1 | Happy path onboarding | Onboard customer → open CHECKING account → deposit $500 | Balance shows $500, appears in audit log |
| 2 | Overdraft rejection | Withdraw more than balance | 422 error, balance unchanged, no transaction row created |
| 3 | Successful transfer | Transfer $100 between two valid accounts | Both balances update, two transaction rows (TRANSFER_OUT/TRANSFER_IN) created |
| 4 | Failed transfer leaves no partial state | Transfer more than sender's balance | 422 error, **both** accounts unchanged (verifies atomicity, not just sender-side) |
| 5 | Large-transaction flag | Deposit/withdraw over $10,000 | Transaction marked `flagged = true` with a reason string |
| 6 | Velocity flag | Fire 6+ transactions on one account within 10 minutes | 6th+ transaction flagged for velocity |
| 7 | Duplicate customer rejection | Onboard two customers with the same email | Second attempt returns 409 Conflict |
| 8 | Auth enforcement | Call `/api/accounts` with no/invalid credentials | 401/403 returned, no data leaked |
| 9 | Audit trail completeness | Perform steps 1–3 above, then GET `/api/reports/audit-log` | Every action from steps 1–3 appears with correct entity/action/timestamp |

## 4. Regression Checklist Before Each Commit
- [ ] `mvn test` passes
- [ ] App boots cleanly (`mvn spring-boot:run`) with no schema errors
- [ ] Frontend can reach the API (check auth status dot turns green)
- [ ] Manual scenarios #2 and #4 re-verified (these protect the core atomicity guarantee)
