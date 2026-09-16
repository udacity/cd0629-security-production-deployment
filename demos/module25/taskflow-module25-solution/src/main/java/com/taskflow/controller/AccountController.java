package com.taskflow.controller;

import com.taskflow.dto.AccountDTO;
import com.taskflow.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Module 13 solution: both problems from the starter are fixed here.
 * The query is parameterized (Injection), and the response returns
 * AccountDTO, never the raw entity, so internalRiskScore structurally
 * cannot leak (Insecure Design).
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/search")
    public List<AccountDTO> search(@RequestParam String name) {
        return accountService.searchByNameSafe(name);
    }
}

