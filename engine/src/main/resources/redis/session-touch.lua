-- Session Touch 脚本
-- KEYS[1]  = 会话 key
-- ARGV[1]  = threshold（秒，字符串）：剩余 TTL <= threshold 时续期
-- ARGV[2]  = timeout（秒，字符串）：续期后的新 TTL
-- 返回值：
--   0  成功（已续期或未到续期间隔）
--  -1  key 存在但无过期时间（异常，已 DEL）
--  -2  key 不存在或已过期
--
-- 将 TTL 校验与条件 EXPIRE 合并为原子操作，避免两次命令之间的竞态。
-- 调用方须以字符串传入 ARGV（StringRedisTemplate 的序列化器要求）。
local ttl = redis.call('TTL', KEYS[1])

if ttl == -2 then
    return -2
end

if ttl == -1 then
    -- key 无过期时间，不属于正常情况，删除并返回异常
    redis.call('DEL', KEYS[1])
    return -1
end

local threshold = tonumber(ARGV[1])
local timeout = tonumber(ARGV[2])

if ttl <= threshold then
    redis.call('EXPIRE', KEYS[1], timeout)
end

return 0

