package com.example.demo.payment;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 基于 Redis ZSET + Lua 脚本的滑动窗口计数器。
 * <p>
 * 数据结构：
 * <ul>
 *   <li>{@link #ORDER_KEY} — 订单创建事件，member=orderId，score=createTime（毫秒）</li>
 *   <li>{@link #PAYMENT_KEY} — 支付成功事件，member=transactionId，score=createTime（毫秒）</li>
 * </ul>
 * Lua 保证「写入 + 过期清理」以及「双 ZSET 统计」在同一服务端原子执行。
 */
@Service
public class PaymentRatioLuaCounter {

    /** 订单滑动窗口 ZSET key */
    public static final String ORDER_KEY = "demo:b2b:order";

    /** 成功支付滑动窗口 ZSET key */
    public static final String PAYMENT_KEY = "demo:b2b:payment:success";

    /** 最长保留时长，超出部分在写入/统计时由 Lua 脚本清理 */
    private static final Duration MAX_RETENTION = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;

    /** 写入单条事件：ZADD + ZREMRANGEBYSCORE，见 redis/lua/record_event.lua */
    private final DefaultRedisScript<Long> recordScript;

    /** 统计窗口内订单数与支付数：见 redis/lua/window_stats.lua */
    private final DefaultRedisScript<List<Long>> windowStatsScript;

    public PaymentRatioLuaCounter(StringRedisTemplate redisTemplate) throws IOException {
        this.redisTemplate = redisTemplate;
        this.recordScript = loadScript("redis/lua/record_event.lua", Long.class);
        this.windowStatsScript = loadScript("redis/lua/window_stats.lua", List.class);
    }

    /**
     * 记录一笔订单创建事件。
     *
     * @param orderId      订单号，作为 ZSET member
     * @param eventTimeMs  订单创建时间（毫秒时间戳），作为 ZSET score
     */
    public void recordOrder(String orderId, long eventTimeMs) {
        executeRecord(ORDER_KEY, orderId, eventTimeMs);
    }

    /**
     * 记录一笔支付成功事件。
     *
     * @param transactionId 支付流水号，作为 ZSET member
     * @param eventTimeMs   支付成功时间（毫秒时间戳），作为 ZSET score
     */
    public void recordSuccessPayment(String transactionId, long eventTimeMs) {
        executeRecord(PAYMENT_KEY, transactionId, eventTimeMs);
    }

    /**
     * 统计以当前时刻为右边界、给定窗口长度内的支付占比。
     */
    public PaymentRatioWindowStats statsForWindow(Duration window) {
        return statsForWindow(window, System.currentTimeMillis());
    }

    /**
     * 统计指定时间窗口内的支付占比。
     * <p>
     * 支付占比 = 成功支付数 / 订单数；未支付占比 = (订单数 - 成功支付数) / 订单数。
     *
     * @param window 窗口长度，如 30 分钟、2 小时
     * @param nowMs  窗口右边界（毫秒时间戳）
     */
    public PaymentRatioWindowStats statsForWindow(Duration window, long nowMs) {
        long windowStart = nowMs - window.toMillis();
        long retentionCutoff = nowMs - MAX_RETENTION.toMillis();

        // Lua 原子返回 [orderCount, paymentCount]，并顺带清理超过 MAX_RETENTION 的过期数据
        List<Long> counts = redisTemplate.execute(
                windowStatsScript,
                List.of(ORDER_KEY, PAYMENT_KEY),
                String.valueOf(windowStart),
                String.valueOf(nowMs),
                String.valueOf(retentionCutoff)
        );

        long orderCount = counts == null || counts.isEmpty() ? 0L : counts.get(0);
        long paymentCount = counts == null || counts.size() < 2 ? 0L : counts.get(1);

        double paymentRatio = orderCount == 0 ? 0.0 : (double) paymentCount / orderCount;
        double unpaidRatio = orderCount == 0 ? 0.0 : (double) (orderCount - paymentCount) / orderCount;

        return new PaymentRatioWindowStats(window, orderCount, paymentCount, paymentRatio, unpaidRatio);
    }

    /** 清空 Redis 中的订单与支付统计（测试或重置时使用） */
    public void clearAll() {
        redisTemplate.delete(List.of(ORDER_KEY, PAYMENT_KEY));
    }

    /**
     * 调用 record_event.lua 写入 ZSET 并清理过期 member。
     *
     * @param key         目标 ZSET key
     * @param member      业务 ID（订单号或流水号）
     * @param eventTimeMs 事件时间戳（毫秒）
     */
    private void executeRecord(String key, String member, long eventTimeMs) {
        long retentionCutoff = eventTimeMs - MAX_RETENTION.toMillis();
        redisTemplate.execute(
                recordScript,
                List.of(key),
                member,
                String.valueOf(eventTimeMs),
                String.valueOf(retentionCutoff)
        );
    }

    /** 从 classpath 加载 Lua 脚本并封装为 {@link DefaultRedisScript} */
    @SuppressWarnings("unchecked")
    private static <T> DefaultRedisScript<T> loadScript(String path, Class<?> resultType) throws IOException {
        String script = StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType((Class<T>) resultType);
        return redisScript;
    }
}
