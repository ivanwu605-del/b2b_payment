package com.example.b2bpayment.api;

import com.example.b2bpayment.merchant.InvalidMerchantStatusException;
import com.example.b2bpayment.merchant.MerchantDisabledException;
import com.example.b2bpayment.merchant.MerchantNotFoundException;
import com.example.b2bpayment.merchant.MerchantReferencedException;
import com.example.b2bpayment.payment.MerchantMismatchException;
import com.example.b2bpayment.payment.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleOrderNotFound(OrderNotFoundException exception) {
        return new ApiErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleMerchantNotFound(MerchantNotFoundException exception) {
        return new ApiErrorResponse(exception.getMessage());
    }

    @ExceptionHandler(MerchantMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMerchantMismatch(MerchantMismatchException exception) {
        return new ApiErrorResponse(exception.getMessage());
    }

    @ExceptionHandler({
            MerchantDisabledException.class,
            MerchantReferencedException.class,
            InvalidMerchantStatusException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleBadRequest(RuntimeException exception) {
        return new ApiErrorResponse(exception.getMessage());
    }
}
