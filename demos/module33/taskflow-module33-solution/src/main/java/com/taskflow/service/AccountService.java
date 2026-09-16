package com.taskflow.service;

import com.taskflow.dto.AccountDTO;
import com.taskflow.dto.AccountTransactionSummary;
import com.taskflow.model.Account;
import com.taskflow.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    @PersistenceContext
    private EntityManager entityManager;

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * RETIRED — kept only so the Injection row in Module 13's Solution
     * has something to point back at and contrast against. Nothing
     * calls this anymore; AccountController now uses searchByNameSafe.
     */
    public List<Account> searchByNameVulnerable(String name) {
        String jpql = "SELECT a FROM Account a WHERE a.name = '" + name + "'";
        return entityManager.createQuery(jpql, Account.class).getResultList();
    }

    /**
     * Module 13, Injection mitigation: delegates to the repository's
     * @Query method, where :name is bound as a real parameter, never
     * concatenated into the query text.
     *
     * Module 33: also now cached. The same name searched twice in a
     * row hits the database once — the second call is served straight
     * from memory. Cache key is the search name itself.
     */
    @Cacheable("accountSearch")
    public List<AccountDTO> searchByNameSafe(String name) {
        return accountRepository.findByNameSafe(name).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Module 33, DELIBERATELY VULNERABLE to N+1: findAll() runs one
     * query for the accounts. Then, for every single account,
     * .getTransactions() runs ANOTHER separate query, lazily, the
     * moment it's touched — one query becomes account-count-plus-one
     * queries, exactly what Module 32's N+1 slide described.
     */
    public List<AccountTransactionSummary> transactionSummaryVulnerable() {
        List<Account> accounts = accountRepository.findAll();
        return accounts.stream()
                .map(a -> new AccountTransactionSummary(a.getName(), a.getTransactions().size()))
                .toList();
    }

    /**
     * Module 33, the fix: findAllWithTransactions() already has every
     * account's transactions loaded by the time this method runs, via
     * the @EntityGraph join. .getTransactions() here touches data
     * that's already in memory — no additional query fires at all.
     */
    public List<AccountTransactionSummary> transactionSummaryFixed() {
        List<Account> accounts = accountRepository.findAllWithTransactions();
        return accounts.stream()
                .map(a -> new AccountTransactionSummary(a.getName(), a.getTransactions().size()))
                .toList();
    }

    /**
     * Module 13, Insecure Design mitigation: internalRiskScore never
     * makes it into this object, so it's structurally impossible for
     * it to leak through this path, not just a matter of remembering
     * to leave a field out.
     */
    private AccountDTO toDTO(Account account) {
        return new AccountDTO(account.getId(), account.getName(), account.getBalance());
    }
}


