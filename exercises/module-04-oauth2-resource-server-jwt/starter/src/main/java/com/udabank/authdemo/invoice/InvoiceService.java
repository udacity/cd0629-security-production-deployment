package com.udabank.authdemo.invoice;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/**
 * Yusuf's near-miss: a partner integration that was only ever supposed to
 * have read access successfully created a duplicate invoice in the sandbox,
 * because nothing here checks what a caller's token actually grants —
 * only that a bearer token was present at all (that check lives in
 * SecurityConfig and is already working).
 *
 * TODO 1: annotate listInvoices() and getInvoice(id) with @PreAuthorize so
 *         they require the "SCOPE_read" authority.
 *         @PreAuthorize("hasAuthority('SCOPE_read')")
 *
 * TODO 2: annotate createInvoice(...) with @PreAuthorize so it requires
 *         the "SCOPE_write" authority — read access alone shouldn't be
 *         enough to create anything.
 *         @PreAuthorize("hasAuthority('SCOPE_write')")
 *
 * These authorities come from the "entitlements" claim on the partner's
 * JWT once you finish the JwtAuthenticationConverter TODO in SecurityConfig —
 * do that one first, or these annotations won't have anything to check yet.
 */
@Service
public class InvoiceService {

    private final Map<Long, Invoice> invoices = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(3);

    public InvoiceService() {
        invoices.put(1L, new Invoice(1L, "Fernbank Bakery", 45_000, "PAID"));
        invoices.put(2L, new Invoice(2L, "Ridgeline Contracting", 128_000, "SENT"));
    }

    // TODO 1: add @PreAuthorize here
    public List<Invoice> listInvoices() {
        return List.copyOf(invoices.values());
    }

    // TODO 1: add @PreAuthorize here
    public Invoice getInvoice(Long id) {
        Invoice invoice = invoices.get(id);
        if (invoice == null) {
            throw new NoSuchElementException("No invoice with id " + id);
        }
        return invoice;
    }

    // TODO 2: add @PreAuthorize here
    public Invoice createInvoice(CreateInvoiceRequest request) {
        Long id = nextId.getAndIncrement();
        Invoice invoice = new Invoice(id, request.customerName(), request.amountCents(), "DRAFT");
        invoices.put(id, invoice);
        return invoice;
    }
}
