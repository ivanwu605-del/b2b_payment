package com.example.demo.api;

public record CreatePaymentRequest(
        String orderId,
        String merchantId,
        String channel,
        String status
) {
}
