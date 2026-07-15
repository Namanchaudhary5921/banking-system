-- ============================================================
-- Stored procedures, triggers, and views for the PostgreSQL schema.
-- These demonstrate the same atomicity/audit guarantees the Java
-- TransactionService enforces in the app layer, done at the database
-- layer instead - useful talking point: defense in depth, and a
-- fallback if the app layer is bypassed (e.g. direct DB access, batch jobs).
-- ============================================================

-- ---------------------------------------------------------------
-- 1. sp_transfer_funds
--    Atomically moves money between two accounts. Uses row locking
--    (FOR UPDATE) to avoid double-spend races, and rolls back the
--    entire transaction automatically if any step raises an exception.
-- ---------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_transfer_funds(
    p_from_account   VARCHAR,
    p_to_account     VARCHAR,
    p_amount         NUMERIC
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_from_balance   NUMERIC(19,4);
    v_from_id        BIGINT;
    v_to_id          BIGINT;
    v_to_balance     NUMERIC(19,4);
BEGIN
    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Transfer amount must be positive';
    END IF;

    IF p_from_account = p_to_account THEN
        RAISE EXCEPTION 'Cannot transfer to the same account';
    END IF;

    -- Lock rows in a consistent order (by account_number) to prevent deadlocks
    -- when two transfers run in opposite directions concurrently.
    SELECT id, balance INTO v_from_id, v_from_balance
    FROM accounts WHERE account_number = LEAST(p_from_account, p_to_account) FOR UPDATE;

    SELECT id, balance INTO v_to_id, v_to_balance
    FROM accounts WHERE account_number = GREATEST(p_from_account, p_to_account) FOR UPDATE;

    -- Re-resolve which locked row is actually "from" vs "to"
    IF p_from_account = LEAST(p_from_account, p_to_account) THEN
        -- v_from_id/v_from_balance already correct
        NULL;
    ELSE
        -- swap: the row fetched first was actually the "to" account
        v_from_balance := v_to_balance;
        v_from_id      := v_to_id;
        SELECT id, balance INTO v_to_id, v_to_balance FROM accounts WHERE account_number = p_to_account;
    END IF;

    IF v_from_balance < p_amount THEN
        RAISE EXCEPTION 'Insufficient funds in account %', p_from_account;
    END IF;

    UPDATE accounts SET balance = balance - p_amount, version = version + 1 WHERE account_number = p_from_account;
    UPDATE accounts SET balance = balance + p_amount, version = version + 1 WHERE account_number = p_to_account;

    INSERT INTO transactions (account_id, type, amount, balance_after, related_account_number)
    SELECT id, 'TRANSFER_OUT', p_amount, balance, p_to_account FROM accounts WHERE account_number = p_from_account;

    INSERT INTO transactions (account_id, type, amount, balance_after, related_account_number)
    SELECT id, 'TRANSFER_IN', p_amount, balance, p_from_account FROM accounts WHERE account_number = p_to_account;

    -- No explicit COMMIT needed inside a procedure invoked within a transaction;
    -- if any RAISE EXCEPTION above fires, Postgres rolls back everything in this block.
END;
$$;

-- ---------------------------------------------------------------
-- 2. trg_audit_transactions
--    Every insert into transactions automatically writes an audit_log
--    row. This guarantees an audit trail even if a caller bypasses the
--    application's AuditService (e.g. an ad hoc script or batch job).
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_audit_transaction() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action, details, performed_by)
    VALUES (
        'TRANSACTION',
        NEW.id::text,
        NEW.type,
        format('amount=%s balance_after=%s', NEW.amount, NEW.balance_after),
        'DB_TRIGGER'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_transactions
AFTER INSERT ON transactions
FOR EACH ROW EXECUTE FUNCTION fn_audit_transaction();

-- ---------------------------------------------------------------
-- 3. trg_prevent_negative_balance
--    Belt-and-suspenders check: even if application logic has a bug,
--    the database itself refuses to let a balance go negative.
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_prevent_negative_balance() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.balance < 0 THEN
        RAISE EXCEPTION 'Balance cannot go negative for account %', NEW.account_number;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_negative_balance
BEFORE UPDATE ON accounts
FOR EACH ROW EXECUTE FUNCTION fn_prevent_negative_balance();

-- ---------------------------------------------------------------
-- 4. Reporting views
-- ---------------------------------------------------------------

-- Quick per-customer balance summary (mirrors /api/reports/summary in the app)
CREATE OR REPLACE VIEW vw_customer_account_summary AS
SELECT
    c.id                      AS customer_id,
    c.first_name || ' ' || c.last_name AS customer_name,
    COUNT(a.id)                AS number_of_accounts,
    COALESCE(SUM(a.balance), 0) AS total_balance
FROM customers c
LEFT JOIN accounts a ON a.customer_id = c.id AND a.active = TRUE
GROUP BY c.id, customer_name;

-- Suspicious activity view for the risk/fraud team
CREATE OR REPLACE VIEW vw_flagged_transactions AS
SELECT
    t.id, a.account_number, c.first_name || ' ' || c.last_name AS customer_name,
    t.type, t.amount, t.flag_reason, t."timestamp"
FROM transactions t
JOIN accounts a  ON a.id = t.account_id
JOIN customers c ON c.id = a.customer_id
WHERE t.flagged = TRUE
ORDER BY t."timestamp" DESC;
