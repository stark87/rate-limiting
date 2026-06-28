local uuid = tostring(ARGV[1])
local timestamp = tonumber(ARGV[2])
local lbound = tonumber(ARGV[3])


local key = KEYS[1]

redis.call('ZREMRANGEBYSCORE', key, '-inf', lbound)

redis.call('ZADD', key, timestamp, uuid)

return redis.call('ZCOUNT', key, lbound, timestamp)

