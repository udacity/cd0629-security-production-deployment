package com.taskflow.dto;

/**
 * Module 33: deliberately minimal — just enough to prove the N+1 fix
 * actually returns the same data, without pulling internalRiskScore
 * into this response path either.
 */
public class AccountTransactionSummary {

    private final String accountName;
    private final int transactionCount;

    public AccountTransactionSummary(String accountName, int transactionCount) {
        this.accountName = accountName;
        this.transactionCount = transactionCount;
    }

    public String getAccountName() {
        return accountName;
    }

    public int getTransactionCount() {
        return transactionCount;
    }
}
