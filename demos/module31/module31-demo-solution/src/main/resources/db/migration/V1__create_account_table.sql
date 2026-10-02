-- Module 31: Flyway now owns this schema. Matches the Account entity's
-- exact fields — Hibernate's ddl-auto is set to "validate" as of this
-- module, meaning it checks this table matches the entity, but no
-- longer creates or drops anything itself.
CREATE TABLE account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    balance DECIMAL(19, 2),
    internal_risk_score INTEGER
);
