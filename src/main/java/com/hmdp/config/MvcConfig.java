package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefershTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器，拦截需要登录的路径，并设置优先级最低（为1，值越大优先级越低）
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns("/user/code","/user/login","/voucher/**","/blog/hot","/shop/**","/shop-type/**","/upload/**").order(1);
        // token刷新的拦截器，拦截一切路径，并设置优先级最高（为0，值越小优先级越高）
        registry.addInterceptor(new RefershTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }


}
