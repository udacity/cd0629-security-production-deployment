package com.udabank.authdemo.invoice;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

    private final Map<Long, Invoice> invoices = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(3);

    public InvoiceService() {
        invoices.put(1L, new Invoice(1L, "Fernbank Bakery", 45_000, "PAID"));
        invoices.put(2L, new Invoice(2L, "Ridgeline Contracting", 128_000, "SENT"));
    }

    @PreAuthorize("hasAuthority('SCOPE_read')")
    public List<Invoice> listInvoices() {
        return List.copyOf(invoices.values());
    }

    @PreAuthorize("hasAuthority('SCOPE_read')")
    public Invoice getInvoice(Long id) {
        Invoice invoice = invoices.get(id);
        if (invoice == null) {
            throw new NoSuchElementException("No invoice with id " + id);
        }
        return invoice;
    }

    @PreAuthorize("hasAuthority('SCOPE_write')")
    public Invoice createInvoice(CreateInvoiceRequest request) {
        Long id = nextId.getAndIncrement();
        Invoice invoice = new Invoice(id, request.customerName(), request.amountCents(), "DRAFT");
        invoices.put(id, invoice);
        return invoice;
    }
}
