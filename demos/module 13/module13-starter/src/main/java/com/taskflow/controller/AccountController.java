package com.taskflow.controller;

import com.taskflow.model.Account;
import com.taskflow.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * DELIBERATELY VULNERABLE — Module 13's starter state.
 *
 * Two problems at once, on the same endpoint: the query is built by
 * string concatenation (Injection), and the response returns the raw
 * Account entity, including internalRiskScore, a field that should
 * never leave this service (Insecure Design).
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/search")
    public List<Account> search(@RequestParam String name) {
        return accountService.searchByNameVulnerable(name);
    }
}
