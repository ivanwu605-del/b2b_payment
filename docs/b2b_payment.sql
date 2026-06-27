/*
 Navicat Premium Dump SQL

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : b2b_payment

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 26/06/2026 17:58:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `order_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `merchant_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `out_trade_no` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` bigint NOT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT 'CNY',
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `update_time` datetime(6) NOT NULL,
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_merchant_out` (`merchant_id`,`out_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for t_payment_transaction
-- ----------------------------
DROP TABLE IF EXISTS `t_payment_transaction`;
CREATE TABLE `t_payment_transaction` (
  `transaction_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `merchant_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channel` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel_order_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `channel_transaction_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` bigint DEFAULT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime(6) NOT NULL,
  `update_time` datetime(6) NOT NULL,
  PRIMARY KEY (`transaction_id`),
  UNIQUE KEY `uk_channel_txn` (`channel_transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
