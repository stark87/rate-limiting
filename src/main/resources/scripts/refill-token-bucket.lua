local limit = tonumber(ARGV[1])
local fillRate = tonumber(ARGV[2])
local key = KEYS[1]

local current = tonumber(redis.call('GET', key .. ":tokens"))

if current == nil then
    return
end

local newValue = math.min(limit, current + fillRate)
redis.call('SET', key .. ":tokens", newValue)