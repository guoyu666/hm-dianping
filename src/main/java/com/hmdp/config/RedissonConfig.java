package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 配置
        Config config = new Config();
        // 这里由于虚拟机打开耗时且麻烦，使用本地Redis，并设置密码
        config.useSingleServer().setAddress("redis://localhost:6379").setPassword("13801874064guoyu");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
