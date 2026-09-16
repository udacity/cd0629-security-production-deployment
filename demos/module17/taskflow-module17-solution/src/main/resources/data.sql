-- Module 13 demo data. admin1's balance and risk score are the "prize"
-- the Injection demo proves you can steal with a single crafted search.
INSERT INTO account (name, balance, internal_risk_score) VALUES ('dev1', 500.00, 10);
INSERT INTO account (name, balance, internal_risk_score) VALUES ('manager1', 10000.00, 95);
INSERT INTO account (name, balance, internal_risk_score) VALUES ('admin1', 250000.00, 99);
