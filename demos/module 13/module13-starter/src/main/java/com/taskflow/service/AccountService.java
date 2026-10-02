package com.taskflow.service;

import com.taskflow.model.Account;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * DELIBERATELY VULNERABLE — Module 13's Injection demo.
     *
     * This is how injection actually happens in real codebases: not by
     * someone intentionally writing something dangerous, but by reaching
     * past Spring Data's safe query methods "just this once" for a
     * dynamic search, and building the query as a string.
     */
    public List<Account> searchByNameVulnerable(String name) {
        String jpql = "SELECT a FROM Account a WHERE a.name = '" + name + "'";
        return entityManager.createQuery(jpql, Account.class).getResultList();
    }
}
