package com.udabank.authdemo.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SupportNoteController {

    private final CustomerRepository customerRepository;
    private final SupportNoteRepository supportNoteRepository;

    public SupportNoteController(CustomerRepository customerRepository, SupportNoteRepository supportNoteRepository) {
        this.customerRepository = customerRepository;
        this.supportNoteRepository = supportNoteRepository;
    }

    @GetMapping("/support/customers/{id}")
    public String viewNotes(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerRepository.findById(id).orElseThrow());
        model.addAttribute("notes", supportNoteRepository.findByCustomerId(id));
        return "customer-notes";
    }
}
