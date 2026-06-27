package com.example.b2bpayment.api;

public record CreateOrderRequest(
        String merchantId,
        String outTradeNo,
        long amount,
        String currency
) {
}
