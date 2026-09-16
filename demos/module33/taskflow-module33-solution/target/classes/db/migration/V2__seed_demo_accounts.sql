-- Module 31: this used to be data.sql (Module 13). Real migration tools
-- treat seed/reference data the same way as schema changes — tracked,
-- ordered, and version-controlled, not a separate ad hoc file. admin1's
-- balance and risk score are the "prize" the Injection demo (Module 13)
-- proves you can steal with a single crafted search.
INSERT INTO account (name, balance, internal_risk_score) VALUES ('dev1', 500.00, 10);
INSERT INTO account (name, balance, internal_risk_score) VALUES ('manager1', 10000.00, 95);
INSERT INTO account (name, balance, internal_risk_score) VALUES ('admin1', 250000.00, 99);
