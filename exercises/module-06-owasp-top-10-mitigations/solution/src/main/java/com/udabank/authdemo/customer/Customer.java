package com.udabank.authdemo.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "assigned_agent_username", nullable = false)
    private String assignedAgentUsername;

    protected Customer() {
        // JPA
    }

    public Customer(String fullName, String accountNumber, String assignedAgentUsername) {
        this.fullName = fullName;
        this.accountNumber = accountNumber;
        this.assignedAgentUsername = assignedAgentUsername;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAssignedAgentUsername() {
        return assignedAgentUsername;
    }
}
