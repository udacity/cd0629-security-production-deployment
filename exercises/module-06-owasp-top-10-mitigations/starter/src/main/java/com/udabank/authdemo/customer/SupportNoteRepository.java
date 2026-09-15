package com.udabank.authdemo.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportNoteRepository extends JpaRepository<SupportNote, Long> {

    List<SupportNote> findByCustomerId(Long customerId);
}
