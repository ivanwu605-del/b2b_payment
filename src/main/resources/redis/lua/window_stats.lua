--[[
  window_stats.lua — 滑动窗口支付占比统计

  用途：
    原子统计指定时间窗口内的订单数与成功支付数，并顺带清理过期数据。
    由 PaymentRatioLuaCounter.statsForWindow 调用。

  统计公式（Java 侧计算）：
    支付占比   = payment_count / order_count
    未支付占比 = (order_count - payment_count) / order_count

  参数：
    KEYS[1]  订单 ZSET key（demo:b2b:order）
    KEYS[2]  成功支付 ZSET key（demo:b2b:payment:success）
    ARGV[1]  窗口起始时间戳（毫秒，含）
    ARGV[2]  窗口结束时间戳（毫秒，含，通常为 now）
    ARGV[3]  retention cutoff，清理 score <= 此值的数据（通常为 now - 12h）

  返回值：
    { order_count, payment_count }  — 两个整数，供 Java 计算占比
]]

-- 统计 [windowStart, windowEnd] 闭区间内的订单数
local order_count = redis.call('ZCOUNT', KEYS[1], ARGV[1], ARGV[2])

-- 统计同一窗口内的成功支付笔数
local payment_count = redis.call('ZCOUNT', KEYS[2], ARGV[1], ARGV[2])

-- 统计完成后清理两个 ZSET 中超过保留期的数据
if ARGV[3] ~= nil and ARGV[3] ~= '' then
    redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[3])
    redis.call('ZREMRANGEBYSCORE', KEYS[2], 0, ARGV[3])
end

return {order_count, payment_count}
