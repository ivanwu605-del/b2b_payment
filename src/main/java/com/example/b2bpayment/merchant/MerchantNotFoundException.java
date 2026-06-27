package com.example.b2bpayment.merchant;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(String merchantId) {
        super("Merchant not found: " + merchantId);
    }
}
