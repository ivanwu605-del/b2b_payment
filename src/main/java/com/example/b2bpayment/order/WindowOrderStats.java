package com.example.b2bpayment.order;

import java.time.Duration;

public record WindowOrderStats(
        Duration window,
        long paidCount,
        long unpaidCount,
        double paidRatio,
        double unpaidRatio
) {
    public long totalCount() {
        return paidCount + unpaidCount;
    }

    @Override
    public String toString() {
        return String.format(
                "窗口=%s | 已支付=%d | 未支付=%d | 合计=%d | 已支付占比=%.2f%% | 未支付占比=%.2f%%",
                formatWindow(window),
                paidCount,
                unpaidCount,
                totalCount(),
                paidRatio * 100,
                unpaidRatio * 100
        );
    }

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
