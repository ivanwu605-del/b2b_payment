package com.example.b2bpayment.api;

import com.example.b2bpayment.db.PaymentRecord;
import com.example.b2bpayment.payment.PaymentApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentApplicationService paymentService;

    public PaymentController(PaymentApplicationService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentRecord createPayment(@RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }
}
