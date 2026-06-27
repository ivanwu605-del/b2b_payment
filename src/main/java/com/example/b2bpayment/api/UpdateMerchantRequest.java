package com.example.b2bpayment.api;

public record UpdateMerchantRequest(
        String merchantName,
        String status
) {
}
