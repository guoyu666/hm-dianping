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
        //这里使用的是虚拟机中Linux的IP地址，可以根据自己的情况设置相对应的地址
        config.useSingleServer().setAddress("redis://192.168.255.129:6379").setPassword("13801874064guoyu");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
