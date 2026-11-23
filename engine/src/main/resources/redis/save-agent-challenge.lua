-- KEYS[1] = 注册记录键, KEYS[2] = 挑战状态键
-- ARGV[1] = 生成证明时使用的注册记录字节
-- ARGV[2] = AgentChallenge (32) || ServerChallenge (32) || nonce (12) || tag (16)
-- 返回保存的握手状态，注册已变化或过期时返回 nil。

-- Java 侧读取记录和生成证明期间，注册可能已被替换或删除。
if redis.call('GET', KEYS[1]) ~= ARGV[1] then
    return nil
end

-- 以毫秒继承原记录的剩余有效期，避免握手状态延长注册期限。
local ttl = redis.call('PTTL', KEYS[1])
if ttl <= 0 then
    return nil
end

-- 同一 AgentChallenge 的重试返回原证明，并保留原有效期。
local state = redis.call('GET', KEYS[2])
if state and #state == #ARGV[2] and state:sub(1, 32) == ARGV[2]:sub(1, 32) then
    return state
end

redis.call('SET', KEYS[2], ARGV[2], 'PX', ttl)
return ARGV[2]
