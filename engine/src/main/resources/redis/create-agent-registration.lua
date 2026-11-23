-- Create Agent Registration 脚本
-- 同一用户同一时刻仅允许一个挂起的 registrationId；新建时原子替换旧注册。
--
-- KEYS[1] = Registration KEY          flowloom:agent:registration:{hex(sha256(registrationId))}
-- KEYS[2] = User → Registration KEY   flowloom:agent:registration:user:{userId}
-- ARGV[1] = RegistrationRecord 字节   userId (16) || psk (16)
-- ARGV[2] = TTL（秒，十进制字符串）
--
-- 返回值:
--   1  成功
--  -1  Registration KEY 已存在（registrationId 碰撞）
--  -2  非法 TTL（<=0 或非整数）

local ttl = tonumber(ARGV[2])

-- 参数校验必须在任何写操作之前完成
if not ttl or ttl <= 0 or ttl ~= math.floor(ttl) then
    return -2
end

-- 检查新 Registration KEY 是否已存在
if redis.call('EXISTS', KEYS[1]) == 1 then
    return -1
end

-- 按映射中保存的原始键删除旧注册，兼容历史二进制键。
local old_registration_key = redis.call('GET', KEYS[2])

if old_registration_key then
    redis.call('DEL', old_registration_key)
end

redis.call('DEL', KEYS[2])

-- 写入新的 RegistrationRecord
redis.call('SET', KEYS[1], ARGV[1], 'EX', ttl)

-- 建立用户与当前 Registration KEY 的映射（同 TTL）
redis.call('SET', KEYS[2], KEYS[1], 'EX', ttl)

return 1
