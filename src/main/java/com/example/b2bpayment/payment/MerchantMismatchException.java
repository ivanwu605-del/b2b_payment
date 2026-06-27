package com.example.b2bpayment.payment;

public class MerchantMismatchException extends RuntimeException {

    public MerchantMismatchException(String orderId, String merchantId) {
        super("Merchant does not match order " + orderId + ": " + merchantId);
    }
}
