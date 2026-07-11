local limit = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local key = KEYS[1]

local currentTokens = redis.call('GET', key .. ":tokens")

if not currentTokens then
    redis.log(redis.LOG_NOTICE, "returning, currentTokens:" .. tostring(currentTokens))
    return;
elseif currentTokens == limit then
    return
end


--call time
local time = redis.call('TIME')
local now = tonumber(time[1])

--redis.log(redis.LOG_NOTICE, "now:" .. now)

--get last refill time
local lastRefill = tonumber(redis.call('GET', key .. ":lastRefill"))
--redis.log(redis.LOG_NOTICE, "lastRefill:" .. lastRefill)

--get token to refill
local refill = (now - lastRefill) * refillRate
--redis.log(redis.LOG_NOTICE, "refill:" .. refill)

local refilledTokens = tonumber(currentTokens) + refill
--redis.log(redis.LOG_NOTICE, "refilledTokens:" .. refilledTokens)

redis.call('SET', key .. ":tokens", math.min(limit, refilledTokens))
redis.call('SET', key .. ":lastRefill", now)
