package com.hmdp.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

/*
    拦截器属于SpringMVC中的内容，如果有遗忘，建议回看Notion中的笔记！！
 */
public class LoginInterceptor implements HandlerInterceptor {

    // LoginInterceptor对象是我们自己手动new出来的，不是通过注解构建的。也就是说LoginInterceptor对象并没有被Spring管理，我们需要自己来注入StringRedisTemplate对象。
    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor() {
    }

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 判断是否需要拦截（ThreadLocal中是否有用户!!）
        if (UserHolder.getUser() == null){
            // 没有，需要拦截，设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 有用户，放行
        return true;
    }
}
