package com.udabank.authdemo.invoice;

public record CreateInvoiceRequest(String customerName, long amountCents) {
}
