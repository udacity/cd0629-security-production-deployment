package com.udabank.authdemo.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_notes")
public class SupportNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "author_username", nullable = false)
    private String authorUsername;

    @Column(nullable = false, length = 2000)
    private String body;

    protected SupportNote() {
        // JPA
    }

    public SupportNote(Long customerId, String authorUsername, String body) {
        this.customerId = customerId;
        this.authorUsername = authorUsername;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getBody() {
        return body;
    }
}
