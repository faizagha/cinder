-- Token-bucket rate limiter, executed atomically by Redis.
-- Inputs (passed from Java):
--   KEYS[1] = the bucket key (e.g. "rate:abc123hash...")
--   ARGV[1] = capacity (max tokens the bucket holds)
--   ARGV[2] = refill_rate (tokens per second)
--   ARGV[3] = now (current unix timestamp in milliseconds)
--   ARGV[4] = ttl_ms (how long the key should live in Redis after being touched)
-- Returns:
--   1 if the request is allowed (a token was deducted)
--   0 if rejected (bucket was empty)

local key       = KEYS[1]
local capacity  = tonumber(ARGV[1])
local refill    = tonumber(ARGV[2])  -- tokens per second
local now       = tonumber(ARGV[3])  -- ms
local ttl_ms    = tonumber(ARGV[4])

-- Read current state. HMGET returns a list; both fields may be nil if key doesn't exist.
local state = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens      = tonumber(state[1])
local last_refill = tonumber(state[2])

-- First time we're seeing this caller — start with a full bucket.
if tokens == nil or last_refill == nil then
    tokens = capacity
    last_refill = now
end

-- Compute how much time has passed and how many tokens that earned us.
-- elapsed_seconds = (now - last_refill) / 1000
-- refilled = elapsed_seconds * refill
local elapsed_ms = now - last_refill
local refilled = (elapsed_ms / 1000.0) * refill

-- Add refilled tokens, but never exceed capacity.
tokens = math.min(capacity, tokens + refilled)

-- Try to spend one token.
local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

-- Save state back, no matter what (we updated tokens via refill regardless).
redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
redis.call('PEXPIRE', key, ttl_ms)

return allowed