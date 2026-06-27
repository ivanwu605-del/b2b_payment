package com.example.demo.api;

public record CreateOrderRequest(
        String merchantId,
        String outTradeNo,
        long amount,
        String currency
) {
}
