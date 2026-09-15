package com.udabank.authdemo.customer;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerSearchService customerSearchService;

    public CustomerController(CustomerService customerService, CustomerSearchService customerSearchService) {
        this.customerService = customerService;
        this.customerSearchService = customerSearchService;
    }

    @GetMapping("/{id}")
    public Customer get(@PathVariable Long id) {
        return customerService.getCustomer(id);
    }

    @GetMapping("/search")
    public List<CustomerSearchResult> search(@RequestParam String name) {
        return customerSearchService.searchByName(name);
    }
}
