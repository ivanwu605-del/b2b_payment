package com.example.b2bpayment.order;

import com.example.b2bpayment.api.CreateOrderRequest;
import com.example.b2bpayment.db.OrderJdbcRepository;
import com.example.b2bpayment.db.OrderRecord;
import com.example.b2bpayment.payment.B2bPaymentRatioService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderApplicationService {

    private static final String STATUS_CREATED = "CREATED";

    private final OrderJdbcRepository orderRepository;
    private final B2bPaymentRatioService ratioService;

    public OrderApplicationService(OrderJdbcRepository orderRepository, B2bPaymentRatioService ratioService) {
        this.orderRepository = orderRepository;
        this.ratioService = ratioService;
    }

    public OrderRecord createOrder(CreateOrderRequest request) {
        LocalDateTime now = LocalDateTime.now();
        OrderRecord order = new OrderRecord(
                "ord_" + UUID.randomUUID(),
                request.merchantId(),
                request.outTradeNo(),
                request.amount(),
                request.currency(),
                STATUS_CREATED,
                now,
                now
        );

        orderRepository.insert(order);
        ratioService.onOrderCreated(order.orderId(), order.createTime());
        return order;
    }
}
