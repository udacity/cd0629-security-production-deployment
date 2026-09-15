package com.udabank.authdemo.customer;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("customerSecurity")
public class CustomerSecurity {

    private final CustomerRepository customerRepository;

    public CustomerSecurity(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public boolean isAssignedAgent(Long customerId, Authentication authentication) {
        return customerRepository.findById(customerId)
                .map(customer -> customer.getAssignedAgentUsername().equals(authentication.getName()))
                .orElse(false);
    }
}
