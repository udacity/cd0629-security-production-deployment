package com.taskflow.dto;

import java.math.BigDecimal;

/**
 * Module 13, Insecure Design mitigation: the external shape of an
 * Account. Deliberately excludes internalRiskScore — a field that
 * exists on the entity but should never cross the API boundary.
 */
public class AccountDTO {

    private final Long id;
    private final String name;
    private final BigDecimal balance;

    public AccountDTO(Long id, String name, BigDecimal balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
