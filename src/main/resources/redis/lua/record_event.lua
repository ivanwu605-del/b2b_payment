--[[
  record_event.lua — 滑动窗口单条事件写入

  用途：
    向 ZSET 写入一条订单或支付事件，并清理超出保留期的历史数据。
    由 PaymentRatioLuaCounter 在 recordOrder / recordSuccessPayment 时调用。

  ZSET 约定：
    member = 业务 ID（orderId 或 transactionId）
    score  = 事件时间戳（毫秒），用于滑动窗口 ZCOUNT 统计

  参数：
    KEYS[1]  目标 ZSET key（b2b_payment:b2b:order 或 b2b_payment:b2b:payment:success）
    ARGV[1]  member，业务唯一 ID
    ARGV[2]  score，事件时间戳（毫秒）
    ARGV[3]  retention cutoff，清理 score <= 此值的数据（通常为 now - 12h）

  返回值：
    1（固定，表示写入成功）
]]

-- 写入/更新事件：同一 member 重复写入会更新 score
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- 删除保留期外的过期数据，控制内存占用
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[3])

return 1
