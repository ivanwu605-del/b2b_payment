package com.example.demo.payment;

import java.time.Duration;

/**
 * 滑动窗口内的支付占比统计结果。
 * <p>
 * 分子来自 {@code t_payment_transaction}（成功支付），分母来自 {@code t_order}（订单创建）。
 */
public record PaymentRatioWindowStats(
        /** 统计窗口长度，如 30 分钟、2 小时、12 小时 */
        Duration window,
        /** 窗口内订单总数（分母） */
        long orderCount,
        /** 窗口内成功支付笔数（分子） */
        long successPaymentCount,
        /** 支付占比 = successPaymentCount / orderCount，范围 [0, 1] */
        double paymentRatio,
        /** 未支付占比 = (orderCount - successPaymentCount) / orderCount，范围 [0, 1] */
        double unpaidRatio
) {
    /** 格式化输出，便于 Demo 测试时在控制台查看 */
    @Override
    public String toString() {
        return String.format(
                "窗口=%s | 订单数=%d | 成功支付数=%d | 支付占比=%.2f%% | 未支付占比=%.2f%%",
                formatWindow(window),
                orderCount,
                successPaymentCount,
                paymentRatio * 100,
                unpaidRatio * 100
        );
    }

    /** 将 Duration 转为可读的中文窗口描述 */
    private static String formatWindow(Duration window) {
        long minutes = window.toMinutes();
        if (minutes < 60) {
            return minutes + "分钟";
        }
        long hours = window.toHours();
        if (hours < 24) {
            return hours + "小时";
        }
        return window.toDays() + "天";
    }
}
