package com.example.b2bpayment.merchant;

public class MerchantDisabledException extends RuntimeException {

    public MerchantDisabledException(String merchantId) {
        super("Merchant is disabled: " + merchantId);
    }
}
