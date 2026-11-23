-- 原子删除挂起注册、挑战状态、owner 索引及仍指向该注册的用户索引。
--
-- KEYS[1] = 注册记录键     flowloom:agent:registration:{hex(sha256(registrationId))}
-- KEYS[2] = owner 索引键   flowloom:agent:registration:owner:{hex(sha256(registrationId))}
-- KEYS[3] = 挑战状态键     flowloom:agent:registration:challenge:{hex(sha256(registrationId))}
-- ARGV[1] = 可选的注册记录原始字节，用于仅删除调用方读到的记录
-- 返回删除的键数（0 至 4）。

local registration_key = KEYS[1]
local owner_key = KEYS[2]
local challenge_key = KEYS[3]

if ARGV[1] and redis.call('GET', registration_key) ~= ARGV[1] then
    return 0
end

local user_key = redis.call('GET', owner_key)

-- owner 索引用于定位用户索引。只有用户索引仍指向当前注册时才删除它。
if user_key and redis.call('GET', user_key) == registration_key then
    return redis.call('DEL', registration_key, owner_key, challenge_key, user_key)
end

-- 用户索引缺失或已指向另一笔注册时，只清理目标注册相关的键。
return redis.call('DEL', registration_key, owner_key, challenge_key)
