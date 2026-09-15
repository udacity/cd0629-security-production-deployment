package com.inkwell.authdemo.document;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Given, not part of the exercise: a small bean you'll reference from a
 * @PreAuthorize SpEL expression as @documentSecurity.isOwner(...).
 * The bean name below ("documentSecurity") is what SpEL resolves — it comes
 * from @Component's value, not the class name.
 */
@Component("documentSecurity")
public class DocumentSecurity {

    private final DocumentRepository documentRepository;

    public DocumentSecurity(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public boolean isOwner(Long documentId, Authentication authentication) {
        return documentRepository.findById(documentId)
                .map(document -> document.getOwnerUsername().equals(authentication.getName()))
                .orElse(false);
    }
}
