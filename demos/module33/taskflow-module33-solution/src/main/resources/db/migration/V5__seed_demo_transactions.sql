-- Module 33: several transactions per account — enough that the N+1
-- query burst is actually visible and dramatic in the console log, not
-- just theoretically present.
INSERT INTO transaction (amount, description, account_id) VALUES (25.00, 'Coffee shop', 1);
INSERT INTO transaction (amount, description, account_id) VALUES (1200.00, 'Rent payment', 1);
INSERT INTO transaction (amount, description, account_id) VALUES (45.50, 'Groceries', 1);

INSERT INTO transaction (amount, description, account_id) VALUES (500.00, 'Client invoice', 2);
INSERT INTO transaction (amount, description, account_id) VALUES (89.99, 'Software subscription', 2);
INSERT INTO transaction (amount, description, account_id) VALUES (2500.00, 'Payroll', 2);

INSERT INTO transaction (amount, description, account_id) VALUES (15000.00, 'Wire transfer', 3);
INSERT INTO transaction (amount, description, account_id) VALUES (300.00, 'Office supplies', 3);
INSERT INTO transaction (amount, description, account_id) VALUES (7500.00, 'Vendor payment', 3);
