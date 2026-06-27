package com.example.demo.payment;

import com.example.demo.api.CreatePaymentRequest;
import com.example.demo.db.OrderJdbcRepository;
import com.example.demo.db.OrderRecord;
import com.example.demo.db.PaymentJdbcRepository;
import com.example.demo.db.PaymentRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentApplicationService {

    private final OrderJdbcRepository orderRepository;
    private final PaymentJdbcRepository paymentRepository;
    private final B2bPaymentRatioService ratioService;

    public PaymentApplicationService(
            OrderJdbcRepository orderRepository,
            PaymentJdbcRepository paymentRepository,
            B2bPaymentRatioService ratioService
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.ratioService = ratioService;
    }

    public PaymentRecord createPayment(CreatePaymentRequest request) {
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
