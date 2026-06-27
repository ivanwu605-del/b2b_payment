package com.example.b2bpayment.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class MerchantJdbcRepository {

    private static final RowMapper<MerchantRecord> ROW_MAPPER = (rs, rowNum) -> new MerchantRecord(
            rs.getString("merchant_id"),
            rs.getString("merchant_name"),
            rs.getString("status"),
            rs.getTimestamp("create_time").toLocalDateTime(),
            rs.getTimestamp("update_time").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public MerchantJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(MerchantRecord merchant) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_merchant
                            (merchant_id, merchant_name, status, create_time, update_time)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                merchant.merchantId(),
                merchant.merchantName(),
                merchant.status(),
                Timestamp.valueOf(merchant.createTime()),
                Timestamp.valueOf(merchant.updateTime())
        );
    }

    public Optional<MerchantRecord> findByMerchantId(String merchantId) {
        return jdbcTemplate.query(
                """
                        SELECT merchant_id, merchant_name, status, create_time, update_time
                        FROM t_merchant
                        WHERE merchant_id = ?
                        """,
                ROW_MAPPER,
                merchantId
        ).stream().findFirst();
    }

    public List<MerchantRecord> findAll() {
        return jdbcTemplate.query(
                """
                        SELECT merchant_id, merchant_name, status, create_time, update_time
                        FROM t_merchant
                        ORDER BY create_time
                        """,
                ROW_MAPPER
        );
    }

    public void update(MerchantRecord merchant) {
        jdbcTemplate.update(
                """
                        UPDATE t_merchant
                        SET merchant_name = ?, status = ?, update_time = ?
                        WHERE merchant_id = ?
                        """,
                merchant.merchantName(),
                merchant.status(),
                Timestamp.valueOf(merchant.updateTime()),
                merchant.merchantId()
        );
    }

    public void deleteByMerchantId(String merchantId) {
        jdbcTemplate.update("DELETE FROM t_merchant WHERE merchant_id = ?", merchantId);
    }

    public boolean hasOrderOrPaymentReferences(String merchantId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT
                            (SELECT COUNT(*) FROM t_order WHERE merchant_id = ?)
                            + (SELECT COUNT(*) FROM t_payment_transaction WHERE merchant_id = ?)
                        """,
                Integer.class,
                merchantId,
                merchantId
        );
        return count != null && count > 0;
    }
}
