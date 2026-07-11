local limit = tonumber(ARGV[1])
local key = KEYS[1]

local value = redis.call('GET', key .. ":tokens")

if not value then
    redis.call('SET', key .. ":tokens", limit - 1)
    local time = redis.call('TIME')
    local now = tonumber(time[1])
    redis.call('SET', key .. ":lastRefill", now)
    return limit -1;
end

local res = redis.call('DECR', key .. ":tokens")

if res < 0 then
    redis.call('SET', key .. ":tokens", 0)
end

return res