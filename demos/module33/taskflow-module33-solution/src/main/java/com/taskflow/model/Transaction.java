package com.taskflow.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Module 33: exists specifically to create a genuine N+1 query
 * opportunity. TaskFlow had no entity relationships at all before this
 * — Account was a flat, standalone entity. Real fintech data is never
 * flat like that; an account has many transactions, matching Module
 * 32's own UdaPay transfer example almost exactly.
 */
@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    public Transaction() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
