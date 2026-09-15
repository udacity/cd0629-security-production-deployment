package com.udabank.authdemo.customer;

import java.util.NoSuchElementException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * The pentest's broken-access-control finding lived here — any SUPPORT
 * agent could load any customer's account by guessing the id, regardless
 * of assignment. Already fixed below with the same owner-or-admin
 * @PreAuthorize pattern the team learned to reach for after an earlier
 * incident. Not part of this exercise; nothing to do in this file.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PreAuthorize("hasRole('ADMIN') or @customerSecurity.isAssignedAgent(#id, authentication)")
    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No customer with id " + id));
    }
}
