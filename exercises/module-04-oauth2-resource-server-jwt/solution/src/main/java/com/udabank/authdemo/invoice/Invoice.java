package com.udabank.authdemo.invoice;

public record Invoice(Long id, String customerName, long amountCents, String status) {
}
