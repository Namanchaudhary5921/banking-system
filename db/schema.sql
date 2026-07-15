-- ============================================================
-- Multi-Tier Banking System - PostgreSQL Schema
-- This is the "production-grade" schema referenced in README.md.
-- The Spring Boot app uses H2 + Hibernate auto-DDL for local demo
-- purposes; this file is the canonical, hand-designed schema meant
-- to demonstrate proper relational design, constraints, and indexing.
-- ============================================================

CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    national_id     VARCHAR(50)  NOT NULL UNIQUE,
    phone           VARCHAR(30)  NOT NULL,
    address         VARCHAR(500) NOT NULL,
    onboarded_date  DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE accounts (
    id                BIGSERIAL PRIMARY KEY,
    account_number    VARCHAR(20) NOT NULL UNIQUE,
    customer_id       BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    account_type      VARCHAR(20) NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    balance           NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    interest_rate     NUMERIC(5,4) NOT NULL DEFAULT 0,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE transactions (
    id                      BIGSERIAL PRIMARY KEY,
    account_id              BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    type                    VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT','WITHDRAWAL','TRANSFER_IN','TRANSFER_OUT')),
    amount                  NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    balance_after           NUMERIC(19,4) NOT NULL,
    related_account_number  VARCHAR(20),
    "timestamp"             TIMESTAMP NOT NULL DEFAULT NOW(),
    flagged                 BOOLEAN NOT NULL DEFAULT FALSE,
    flag_reason             VARCHAR(500)
);

CREATE TABLE audit_log (
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(50) NOT NULL,
    entity_id     VARCHAR(50) NOT NULL,
    action        VARCHAR(50) NOT NULL,
    details       VARCHAR(1000),
    performed_by  VARCHAR(100) NOT NULL,
    "timestamp"   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE app_users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(100) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','TELLER','CUSTOMER'))
);

-- ---- Indexes for the query patterns the app actually uses ----
CREATE INDEX idx_accounts_customer_id      ON accounts(customer_id);
CREATE INDEX idx_transactions_account_id   ON transactions(account_id);
CREATE INDEX idx_transactions_timestamp    ON transactions("timestamp");
CREATE INDEX idx_transactions_flagged      ON transactions(flagged) WHERE flagged = TRUE;
CREATE INDEX idx_audit_log_entity          ON audit_log(entity_type, entity_id);
