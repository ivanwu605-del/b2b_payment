package com.example.demo.db;

import java.time.LocalDateTime;

public record PaymentRecord(
        String transactionId,
        String orderId,
        String merchantId,
        String channel,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
