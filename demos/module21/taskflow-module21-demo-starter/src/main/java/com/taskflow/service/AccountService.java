package com.taskflow.service;

import com.taskflow.dto.AccountDTO;
import com.taskflow.model.Account;
import com.taskflow.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
     */
    public List<AccountDTO> searchByNameSafe(String name) {
        return accountRepository.findByNameSafe(name).stream()
                .map(this::toDTO)
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

