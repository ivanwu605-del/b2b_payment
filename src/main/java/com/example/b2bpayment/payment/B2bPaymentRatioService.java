package com.example.b2bpayment.payment;

import com.example.b2bpayment.db.OrderJdbcRepository;
import com.example.b2bpayment.db.OrderRecord;
import com.example.b2bpayment.db.PaymentJdbcRepository;
import com.example.b2bpayment.db.PaymentRecord;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class B2bPaymentRatioService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    private final PaymentRatioLuaCounter luaCounter;
    private final OrderJdbcRepository orderRepository;
    private final PaymentJdbcRepository paymentRepository;

    public B2bPaymentRatioService(
            PaymentRatioLuaCounter luaCounter,
            OrderJdbcRepository orderRepository,
            PaymentJdbcRepository paymentRepository
    ) {
        this.luaCounter = luaCounter;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public void onOrderCreated(String orderId, LocalDateTime createTime) {
        luaCounter.recordOrder(orderId, toEpochMs(createTime));
    }

    public void onPaymentSuccess(String transactionId, LocalDateTime createTime) {
        luaCounter.recordSuccessPayment(transactionId, toEpochMs(createTime));
    }

    public PaymentRatioWindowStats statsForWindow(Duration window) {
        return luaCounter.statsForWindow(window);
    }

    public PaymentRatioWindowStats statsForWindow(Duration window, LocalDateTime now) {
        return luaCounter.statsForWindow(window, toEpochMs(now));
    }

    /**
     * 从 MySQL 回填最近 retention 内的订单与成功支付到 Redis，便于 b2b_payment 或重启后恢复统计。
     */
    public void syncFromDatabase(Duration retention, LocalDateTime now) {
        LocalDateTime since = now.minus(retention);
        for (OrderRecord order : orderRepository.findCreatedSince(since)) {
            onOrderCreated(order.orderId(), order.createTime());
        }
        for (PaymentRecord payment : paymentRepository.findSuccessSince(since)) {
            onPaymentSuccess(payment.transactionId(), payment.createTime());
        }
    }

    public void clearRedisStats() {
        luaCounter.clearAll();
    }

    private static long toEpochMs(LocalDateTime time) {
        return time.atZone(ZONE).toInstant().toEpochMilli();
    }
}
