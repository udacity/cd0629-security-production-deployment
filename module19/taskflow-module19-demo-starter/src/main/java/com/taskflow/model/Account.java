package com.taskflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

/**
 * Module 13: a real JPA entity, backed by an actual (in-memory) database,
 * so Injection has a genuine attack surface.
 *
 * internalRiskScore is deliberately internal-only — a fraud/risk signal
 * that should never leave this service. It exists specifically to give
 * the Insecure Design mitigation (Module 12) something real to protect:
 * returning this entity directly from a controller would leak it.
 */
@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private BigDecimal balance;
    private Integer internalRiskScore;

    public Account() {
    }

    public Account(String name, BigDecimal balance, Integer internalRiskScore) {
        this.name = name;
        this.balance = balance;
        this.internalRiskScore = internalRiskScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Integer getInternalRiskScore() {
        return internalRiskScore;
    }

    public void setInternalRiskScore(Integer internalRiskScore) {
        this.internalRiskScore = internalRiskScore;
    }
}
