package com.inkwell.authdemo.document;

import java.util.NoSuchElementException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @PreAuthorize("hasRole('ADMIN') or @documentSecurity.isOwner(#id, authentication)")
    public Document view(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No document with id " + id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        documentRepository.deleteById(id);
    }
}
