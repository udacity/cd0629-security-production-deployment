package com.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Module 33: @EnableCaching activates Spring's caching abstraction —
 * without this, @Cacheable on AccountService.searchByNameSafe() does
 * nothing at all, silently.
 */
@SpringBootApplication
@EnableCaching
public class TaskFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskFlowApplication.class, args);
    }
}
