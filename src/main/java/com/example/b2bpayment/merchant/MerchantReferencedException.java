package com.example.b2bpayment.merchant;

public class MerchantReferencedException extends RuntimeException {

    public MerchantReferencedException(String merchantId) {
        super("Merchant is referenced by orders or payments: " + merchantId);
    }
}
