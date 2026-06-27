package com.example.demo.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 滑动窗口订单支付占比 Demo。
 * 运行前请确保本地 Redis 已启动：127.0.0.1:6379，database=0。
 *
 * mvn test -Dtest=OrderSlidingWindowCounterTest#slidingWindowPaymentRatioDemo
 */
@SpringBootTest
class OrderSlidingWindowCounterTest {

    @Autowired
    private OrderSlidingWindowCounter counter;

    @BeforeEach
    void setUp() {
        counter.clearAll();
    }

    @Test
    void slidingWindowPaymentRatioDemo() {
        long now = System.currentTimeMillis();

        // 过去 30 分钟内：8 笔已支付，2 笔未支付
        seedOrders(now, Duration.ofMinutes(5), 8, OrderPaymentStatus.PAID, "paid-30m");
        seedOrders(now, Duration.ofMinutes(15), 2, OrderPaymentStatus.UNPAID, "unpaid-30m");

        // 30 分钟 ~ 2 小时之间：5 笔已支付，15 笔未支付（计入 2h / 12h，不计入 30m）
        seedOrders(now, Duration.ofHours(1).plusMinutes(30), 5, OrderPaymentStatus.PAID, "paid-2h");
        seedOrders(now, Duration.ofHours(1).plusMinutes(45), 15, OrderPaymentStatus.UNPAID, "unpaid-2h");

        // 6~10 小时前：20 笔已支付，5 笔未支付（仅计入 12h）
        seedOrders(now, Duration.ofHours(6), 20, OrderPaymentStatus.PAID, "paid-12h");
        seedOrders(now, Duration.ofHours(9), 5, OrderPaymentStatus.UNPAID, "unpaid-12h");

        WindowOrderStats last30Minutes = counter.statsForWindow(Duration.ofMinutes(30), now);
        WindowOrderStats last2Hours = counter.statsForWindow(Duration.ofHours(2), now);
        WindowOrderStats last12Hours = counter.statsForWindow(Duration.ofHours(12), now);

        System.out.println("========== 滑动窗口订单支付占比 Demo ==========");
        System.out.println(last30Minutes);
        System.out.println(last2Hours);
        System.out.println(last12Hours);
        System.out.println("==============================================");

        assertEquals(8, last30Minutes.paidCount());
        assertEquals(2, last30Minutes.unpaidCount());
        assertEquals(0.80, last30Minutes.paidRatio(), 0.001);
        assertEquals(0.20, last30Minutes.unpaidRatio(), 0.001);

        assertEquals(13, last2Hours.paidCount());
        assertEquals(17, last2Hours.unpaidCount());
        assertEquals(13.0 / 30, last2Hours.paidRatio(), 0.001);

        assertEquals(33, last12Hours.paidCount());
        assertEquals(22, last12Hours.unpaidCount());
        assertEquals(33.0 / 55, last12Hours.paidRatio(), 0.001);
        assertTrue(last12Hours.totalCount() > last2Hours.totalCount());
    }

    private void seedOrders(long nowMs, Duration ago, int count, OrderPaymentStatus status, String prefix) {
        long eventTime = nowMs - ago.toMillis();
        for (int i = 0; i < count; i++) {
            counter.recordOrder(prefix + "-" + i, status, eventTime + i);
        }
    }
}
