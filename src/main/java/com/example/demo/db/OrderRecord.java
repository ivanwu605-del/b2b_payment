package com.example.demo.db;

import java.time.LocalDateTime;

public record OrderRecord(
        String orderId,
        String merchantId,
        String outTradeNo,
        long amount,
        String currency,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
