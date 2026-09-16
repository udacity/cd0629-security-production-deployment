-- Module 33: the other half of Account's new transactions
-- relationship. Standard FK to account, matching Module 32's own
-- UdaPay transfer example.
CREATE TABLE transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(19, 2),
    description VARCHAR(255),
    account_id BIGINT,
    FOREIGN KEY (account_id) REFERENCES account(id)
);
