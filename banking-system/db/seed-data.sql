-- Sample data for local PostgreSQL testing of schema.sql / stored-procedures.sql
-- (Run after schema.sql and stored-procedures.sql)

INSERT INTO customers (first_name, last_name, email, national_id, phone, address) VALUES
('Jane', 'Doe',   'jane.doe@example.com',   'NID-1001', '555-0101', '123 Main St, Springfield'),
('John', 'Smith', 'john.smith@example.com', 'NID-1002', '555-0102', '456 Oak Ave, Springfield'),
('Amy',  'Lee',   'amy.lee@example.com',    'NID-1003', '555-0103', '789 Pine Rd, Shelbyville');

INSERT INTO accounts (account_number, customer_id, account_type, balance, interest_rate) VALUES
('4000000001', 1, 'CHECKING', 2500.00, 0.0010),
('4000000002', 1, 'SAVINGS',  10000.00, 0.0150),
('4000000003', 2, 'CHECKING', 750.00, 0.0010),
('4000000004', 3, 'SAVINGS',  5000.00, 0.0150);

-- Example usage of the stored procedure:
-- CALL sp_transfer_funds('4000000001', '4000000003', 100.00);

-- Example: check the reporting views
-- SELECT * FROM vw_customer_account_summary;
-- SELECT * FROM vw_flagged_transactions;
