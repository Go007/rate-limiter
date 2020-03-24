--- 获取令牌
--- 返回码
--- 0 没有令牌桶配置
--- -1 表示取令牌失败，也就是桶里没有令牌
--- 1 表示取令牌成功
--- @param key:令牌的唯一标识
--- @param permits:请求令牌数量
--- @param curr_mill_second:当前毫秒数
--- @param context:使用令牌的应用标识
local function acquire(key, permits, curr_mill_second, context)
    local rate_limit_info = redis.pcall("HMGET", key, "last_mill_second", "curr_permits", "max_permits", "rate", "app")
    local last_mill_second = rate_limit_info[1]
    local curr_permits = tonumber(rate_limit_info[2])
    local max_permits = tonumber(rate_limit_info[3])
    local rate = rate_limit_info[4]
    local app = rate_limit_info[5]

    --- 标识没有配置令牌桶
    if (type(app) == 'boolean' or app == nil or app ~= context) then
        return 0
    end

    local local_curr_permits = max_permits;

    --- 如果首次获取令牌,因为令牌桶刚刚创建，上一次获取令牌的毫秒数为空,直接设置上一次向桶里添加令牌的时间为当前时间
    if (type(last_mill_second) == 'boolean' or last_mill_second == nil) then
        redis.pcall("HSET", key, "last_mill_second", curr_mill_second)
    else
        --- 非首次获取令牌
        --- 根据 当前时间和上一次向桶里添加令牌的时间差，触发式往桶里添加令牌，并且更新上一次向桶里添加令牌的时间
        local reverse_permits = math.floor(((curr_mill_second - last_mill_second) / 1000) * rate)
        local expect_curr_permits = reverse_permits + curr_permits;
        local_curr_permits = math.min(expect_curr_permits, max_permits);

        --- 如果向桶里添加的令牌数 > 0,则更新上一次向桶里添加令牌的时间;如果不足一个，则不更新
        if (reverse_permits > 0) then
            redis.pcall("HSET", key, "last_mill_second", curr_mill_second)
        end
    end

    local result = -1
    if (local_curr_permits - permits >= 0) then
        result = 1
        redis.pcall("HSET", key, "curr_permits", local_curr_permits - permits)
    else
        redis.pcall("HSET", key, "curr_permits", local_curr_permits)
    end

    return result
end

--- 初始化令牌桶配置
--- @param key:令牌的唯一标识
--- @param max_permits:桶大小
--- @param rate:向桶里添加令牌的速率
--- @param app: 使用令牌桶的应用标识
local function init(key, max_permits, rate, app)
    local rate_limit_info = redis.pcall("HMGET", key, "last_mill_second", "curr_permits", "max_permits", "rate", "app")
    local org_max_permits = tonumber(rate_limit_info[3])
    local org_rate = rate_limit_info[4]
    local org_app = rate_limit_info[5]

    if (org_max_permits == nil) or (app ~= org_app or rate ~= org_rate or max_permits ~= org_max_permits) then
        redis.pcall("HMSET", key, "max_permits", max_permits, "rate", rate, "curr_permits", max_permits, "app", app)
    end
    return 1;
end

--- 删除令牌桶
local function delete(key)
    redis.pcall("DEL", key)
    return 1;
end

local key = KEYS[1]
local method = ARGV[1]

if method == 'acquire' then
    return acquire(key, ARGV[2], ARGV[3], ARGV[4])
elseif method == 'init' then
    return init(key, ARGV[2], ARGV[3], ARGV[4])
elseif method == 'delete' then
    return delete(key)
else
    --ignore
end












