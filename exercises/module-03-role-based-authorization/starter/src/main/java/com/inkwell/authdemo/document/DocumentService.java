package com.inkwell.authdemo.document;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

/**
 * Jules's bug report: a customer changed the ID in
 * GET /api/documents/{id} and saw a stranger's contract. Right now
 * NOTHING here checks who's asking — any authenticated user can view or
 * delete any document.
 *
 * TODO 1: annotate view(id) with @PreAuthorize so it only succeeds when the
 *         caller is an ADMIN, OR the caller owns the document. Building
 *         blocks you'll need in the SpEL expression:
 *           - hasRole('ADMIN')                        — role check
 *           - the "or" keyword to combine two conditions
 *           - @documentSecurity.isOwner(???, ???)      — calls the given
 *             DocumentSecurity bean; it needs the document id (this method's
 *             "id" parameter, referenced in SpEL as #id) and the current
 *             Authentication (SpEL exposes it as the variable "authentication")
 *
 * TODO 2: annotate delete(id) with @PreAuthorize so ONLY an ADMIN can call
 *         it — being the owner is not enough. (Inkwell wants a support
 *         paper trail before anything gets deleted, even by its owner.)
 *         Same hasRole(...) building block as above, no "or" needed here.
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // TODO 1: add @PreAuthorize here
    public Document view(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No document with id " + id));
    }

    // TODO 2: add @PreAuthorize here
    public void delete(Long id) {
        documentRepository.deleteById(id);
    }
}
