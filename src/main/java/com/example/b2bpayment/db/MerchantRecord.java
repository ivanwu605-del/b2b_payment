package com.example.b2bpayment.db;

import java.time.LocalDateTime;

public record MerchantRecord(
        String merchantId,
        String merchantName,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
