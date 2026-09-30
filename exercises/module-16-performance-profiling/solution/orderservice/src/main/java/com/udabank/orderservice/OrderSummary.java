package com.udabank.orderservice;

import java.util.List;

public record OrderSummary(String orderId, String sku, List<String> items) {
}
