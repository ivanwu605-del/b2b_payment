package com.example.b2bpayment.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PaymentJdbcRepository {

    public static final String STATUS_SUCCESS = "SUCCESS";

    private static final RowMapper<PaymentRecord> ROW_MAPPER = (rs, rowNum) -> new PaymentRecord(
            rs.getString("transaction_id"),
            rs.getString("order_id"),
            rs.getString("merchant_id"),
            rs.getString("channel"),
            rs.getString("status"),
            rs.getTimestamp("create_time").toLocalDateTime(),
            rs.getTimestamp("update_time").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public PaymentJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(PaymentRecord payment) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_payment_transaction
                            (transaction_id, order_id, merchant_id, channel, status, create_time, update_time)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                payment.transactionId(),
                payment.orderId(),
                payment.merchantId(),
                payment.channel(),
                payment.status(),
                Timestamp.valueOf(payment.createTime()),
                Timestamp.valueOf(payment.updateTime())
        );
    }

    public List<PaymentRecord> findSuccessSince(LocalDateTime since) {
        return jdbcTemplate.query(
                """
                        SELECT transaction_id, order_id, merchant_id, channel, status, create_time, update_time
                        FROM t_payment_transaction
                        WHERE status = ?
                          AND create_time >= ?
                        ORDER BY create_time
                        """,
                ROW_MAPPER,
                STATUS_SUCCESS,
                Timestamp.valueOf(since)
        );
    }

    public int deleteByMerchantIdPrefix(String prefix) {
        return jdbcTemplate.update("DELETE FROM t_payment_transaction WHERE merchant_id LIKE ?", prefix + "%");
    }
}
