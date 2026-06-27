package com.example.b2bpayment.merchant;

public class InvalidMerchantStatusException extends RuntimeException {

    public InvalidMerchantStatusException(String status) {
        super("Invalid merchant status: " + status);
    }
}
