package com.example.b2bpayment.api;

public record CreateMerchantRequest(
        String merchantName,
        String status
) {
}
