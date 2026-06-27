package com.example.b2bpayment.order;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis ZSET 的滑动窗口计数：score 为事件时间戳，member 为订单号。
 * 保留最长 12 小时数据，可按任意更短窗口统计已支付 / 未支付占比。
 */
@Service
public class OrderSlidingWindowCounter {

    public static final String PAID_KEY = "b2b_payment:order:paid";
    public static final String UNPAID_KEY = "b2b_payment:order:unpaid";

    private static final Duration MAX_RETENTION = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;

    public OrderSlidingWindowCounter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordOrder(String orderId, OrderPaymentStatus status) {
        recordOrder(orderId, status, System.currentTimeMillis());
    }

    public void recordOrder(String orderId, OrderPaymentStatus status, long eventTimeMs) {
        String key = keyFor(status);
        redisTemplate.opsForZSet().add(key, orderId, eventTimeMs);
        trimExpired(key, eventTimeMs);
    }

    public long countInWindow(OrderPaymentStatus status, Duration window, long nowMs) {
        long windowStart = nowMs - window.toMillis();
        Long count = redisTemplate.opsForZSet().count(keyFor(status), windowStart, nowMs);
        return count == null ? 0L : count;
    }

    public WindowOrderStats statsForWindow(Duration window) {
        return statsForWindow(window, System.currentTimeMillis());
    }

    public WindowOrderStats statsForWindow(Duration window, long nowMs) {
        long paidCount = countInWindow(OrderPaymentStatus.PAID, window, nowMs);
        long unpaidCount = countInWindow(OrderPaymentStatus.UNPAID, window, nowMs);
        long total = paidCount + unpaidCount;

        double paidRatio = total == 0 ? 0.0 : (double) paidCount / total;
        double unpaidRatio = total == 0 ? 0.0 : (double) unpaidCount / total;

        return new WindowOrderStats(window, paidCount, unpaidCount, paidRatio, unpaidRatio);
    }

    public void clearAll() {
        redisTemplate.delete(List.of(PAID_KEY, UNPAID_KEY));
    }

    private void trimExpired(String key, long nowMs) {
        long cutoff = nowMs - MAX_RETENTION.toMillis();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
    }

    private static String keyFor(OrderPaymentStatus status) {
        return status == OrderPaymentStatus.PAID ? PAID_KEY : UNPAID_KEY;
    }
}
