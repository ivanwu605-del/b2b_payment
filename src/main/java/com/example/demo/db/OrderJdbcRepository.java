package com.example.demo.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OrderJdbcRepository {

    private static final RowMapper<OrderRecord> ROW_MAPPER = (rs, rowNum) -> new OrderRecord(
            rs.getString("order_id"),
            rs.getString("merchant_id"),
            rs.getString("out_trade_no"),
            rs.getLong("amount"),
            rs.getString("currency"),
            rs.getString("status"),
            rs.getTimestamp("create_time").toLocalDateTime(),
            rs.getTimestamp("update_time").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public OrderJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(OrderRecord order) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_order
                            (order_id, merchant_id, out_trade_no, amount, currency, status, create_time, update_time)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                order.orderId(),
                order.merchantId(),
                order.outTradeNo(),
                order.amount(),
                order.currency(),
                order.status(),
                Timestamp.valueOf(order.createTime()),
                Timestamp.valueOf(order.updateTime())
        );
    }

    public List<OrderRecord> findCreatedSince(LocalDateTime since) {
        return jdbcTemplate.query(
                """
                        SELECT order_id, merchant_id, out_trade_no, amount, currency, status, create_time, update_time
                        FROM t_order
                        WHERE create_time >= ?
                        ORDER BY create_time
                        """,
                ROW_MAPPER,
                Timestamp.valueOf(since)
        );
    }

    public int deleteByMerchantIdPrefix(String prefix) {
        return jdbcTemplate.update("DELETE FROM t_order WHERE merchant_id LIKE ?", prefix + "%");
    }
}
