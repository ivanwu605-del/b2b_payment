package com.example.b2bpayment.payment;

import com.example.b2bpayment.api.CreatePaymentRequest;
import com.example.b2bpayment.db.OrderJdbcRepository;
import com.example.b2bpayment.db.OrderRecord;
import com.example.b2bpayment.db.PaymentJdbcRepository;
import com.example.b2bpayment.db.PaymentRecord;
import com.example.b2bpayment.merchant.MerchantApplicationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentApplicationService {

    private final OrderJdbcRepository orderRepository;
    private final PaymentJdbcRepository paymentRepository;
    private final B2bPaymentRatioService ratioService;
    private final MerchantApplicationService merchantService;

    public PaymentApplicationService(
            OrderJdbcRepository orderRepository,
            PaymentJdbcRepository paymentRepository,
            B2bPaymentRatioService ratioService,
            MerchantApplicationService merchantService
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.ratioService = ratioService;
        this.merchantService = merchantService;
    }

    public PaymentRecord createPayment(CreatePaymentRequest request) {
        merchantService.requireActiveMerchant(request.merchantId());
        OrderRecord order = orderRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));
        if (!order.merchantId().equals(request.merchantId())) {
            throw new MerchantMismatchException(request.orderId(), request.merchantId());
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentRecord payment = new PaymentRecord(
                "pay_" + UUID.randomUUID(),
                request.orderId(),
                request.merchantId(),
                request.channel(),
                request.status(),
                now,
                now
        );

        paymentRepository.insert(payment);
        if (PaymentJdbcRepository.STATUS_SUCCESS.equals(payment.status())) {
            ratioService.onPaymentSuccess(payment.transactionId(), payment.createTime());
        }
        return payment;
    }
}
