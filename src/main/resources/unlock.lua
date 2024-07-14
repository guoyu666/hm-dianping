-- 这里的KEYS[1]就是锁的key,ARGV[1]就是当前线程标示
-- 获取锁中的标示，判断是否于当前线程标示一致
if (redis.call("get",KEYS[1]) == ARGV[1]) then
    -- 如果一致，则删除锁
    return redis.call("del",KEYS[1])
end
    -- 如果不一致，则直接返回
    return 0