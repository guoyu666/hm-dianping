package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author GuoYu
 * @since 2024-6-8
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public User findByPhone(String phone) {
        User user = query().eq("phone", phone).one();
        return user;
    }

    @Override
    public User creatUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("用户" + phone); // 昵称也设置为电话
        // 保存用户
        save(user);
        return user;
    }


}
