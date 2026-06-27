package com.example.demo.payment;

import com.example.demo.db.OrderJdbcRepository;
import com.example.demo.db.OrderRecord;
import com.example.demo.db.PaymentJdbcRepository;
import com.example.demo.db.PaymentRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MySQL + Redis Lua 滑动窗口支付占比 Demo。
 *
 * 前置条件：
 * 1. MySQL 127.0.0.1:3306/b2b_payment，已执行 docs/b2b_payment.sql
 * 2. Redis 127.0.0.1:6379
 *
 * 测试结束后 MySQL 数据会保留（merchant_id = demo-test-merchant），便于查看；
 * 下次运行前会自动清理旧数据。
 *
 * mvn test -Dtest=PaymentRatioLuaDemoTest#paymentRatioSlidingWindowDemo
 */
@SpringBootTest
class PaymentRatioLuaDemoTest {

    private static final String MERCHANT_ID = "demo-test-merchant";

    @Autowired
    private B2bPaymentRatioService ratioService;

    @Autowired
    private OrderJdbcRepository orderRepository;

    @Autowired
    private PaymentJdbcRepository paymentRepository;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        clearTestData();
        now = LocalDateTime.now();
    }

    @Test
    void paymentRatioSlidingWindowDemo() {
        seedDatabase(now);

        ratioService.syncFromDatabase(Duration.ofHours(12), now);

        PaymentRatioWindowStats last30Minutes = ratioService.statsForWindow(Duration.ofMinutes(30), now);
        PaymentRatioWindowStats last2Hours = ratioService.statsForWindow(Duration.ofHours(2), now);
        PaymentRatioWindowStats last12Hours = ratioService.statsForWindow(Duration.ofHours(12), now);

        System.out.println("========== MySQL + Redis Lua 支付占比 Demo ==========");
        System.out.println(last30Minutes);
        System.out.println(last2Hours);
        System.out.println(last12Hours);
        System.out.println("====================================================");

        assertEquals(10, last30Minutes.orderCount());
        assertEquals(8, last30Minutes.successPaymentCount());
        assertEquals(0.80, last30Minutes.paymentRatio(), 0.001);
        assertEquals(0.20, last30Minutes.unpaidRatio(), 0.001);

        assertEquals(30, last2Hours.orderCount());
        assertEquals(13, last2Hours.successPaymentCount());
        assertEquals(13.0 / 30, last2Hours.paymentRatio(), 0.001);

        assertEquals(55, last12Hours.orderCount());
        assertEquals(33, last12Hours.successPaymentCount());
        assertEquals(33.0 / 55, last12Hours.paymentRatio(), 0.001);
        assertTrue(last12Hours.orderCount() > last2Hours.orderCount());
    }

    private void seedDatabase(LocalDateTime baseTime) {
        // 过去 30 分钟：10 笔订单，8 笔成功支付
        for (int i = 0; i < 10; i++) {
            LocalDateTime createTime = baseTime.minusMinutes(20 - i);
            String orderId = "demo-order-30m-" + i;
            insertOrder(orderId, createTime);
            if (i < 8) {
                insertPayment("demo-pay-30m-" + i, orderId, createTime.plusSeconds(30));
            }
        }

        // 30 分钟 ~ 2 小时：20 笔订单，5 笔成功支付
        for (int i = 0; i < 20; i++) {
            LocalDateTime createTime = baseTime.minusMinutes(90 - i);
            String orderId = "demo-order-2h-" + i;
            insertOrder(orderId, createTime);
            if (i < 5) {
                insertPayment("demo-pay-2h-" + i, orderId, createTime.plusSeconds(30));
            }
        }

        // 2 小时 ~ 12 小时：25 笔订单，20 笔成功支付
        for (int i = 0; i < 25; i++) {
            LocalDateTime createTime = baseTime.minusHours(8).plusMinutes(i);
            String orderId = "demo-order-12h-" + i;
            insertOrder(orderId, createTime);
            if (i < 20) {
                insertPayment("demo-pay-12h-" + i, orderId, createTime.plusSeconds(30));
            }
        }
    }

    private void insertOrder(String orderId, LocalDateTime createTime) {
        orderRepository.insert(new OrderRecord(
                orderId,
                MERCHANT_ID,
                "out-" + orderId,
                100L,
                "CNY",
                "CREATED",
                createTime,
                createTime
        ));
    }

    private void insertPayment(String transactionId, String orderId, LocalDateTime createTime) {
        paymentRepository.insert(new PaymentRecord(
                transactionId,
                orderId,
                MERCHANT_ID,
                "WECHAT",
                PaymentJdbcRepository.STATUS_SUCCESS,
                createTime,
                createTime
        ));
    }

    /**
     * 每次运行前清理，保证可重复执行；测试结束后不删库，便于查看数据。
     */
    private void clearTestData() {
        ratioService.clearRedisStats();
        paymentRepository.deleteByMerchantIdPrefix(MERCHANT_ID);
        orderRepository.deleteByMerchantIdPrefix(MERCHANT_ID);
    }
}
