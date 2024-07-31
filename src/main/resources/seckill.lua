-- 1.参数列表
-- 1.1 优惠卷id
local voucherId = ARGV[1]
-- 1.2 用户id
local userId = ARGV[2]

-- 2. 数据key
-- 2.1 秒杀卷库存key
local stockKey = "seckill:stock:".. voucherId
-- 2.2 订单key
local orderKey = "seckill:order".. voucherId

-- 3. 脚本业务
-- 3.1 判断库存是否充足
if(tonumber(redis.call("get", stockKey)) <= "0") then
    -- 3.2 库存不足，返回1
    return 1
end

-- 3.3 判断用户是否下单
if(redis.call("sismember", orderKey, userId)) then
    -- 3.4 用户已下单，返回2
    return 2
end

-- 3.5 减库存，添加用户到订单列表
redis.call("decr", stockKey) -- decr命令让key中存储的数字值减去1
redis.call("sadd", orderKey, userId)

-- 3.6 返回0，表示秒杀成功
return 0