package com.example.b2bpayment.api;

public record CreatePaymentRequest(
        String orderId,
        String merchantId,
        String channel,
        String status
) {
}
