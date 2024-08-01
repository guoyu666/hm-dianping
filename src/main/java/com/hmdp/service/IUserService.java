package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.User;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author GuoYu
 * @since 2024-6-7
 */
public interface IUserService extends IService<User> {

    // 根据手机号查找
    User findByPhone(String phone);

    // 创建用户
    User creatUser(String phone);

}
