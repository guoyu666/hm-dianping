package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author GuoYu
 * @since 2024-6-7
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private JavaMailSender mailSender;  // 邮件

    @Value("${spring.mail.username}")
    private String MyFrom;

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送邮件验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 验证邮箱号
        if (RegexUtils.isEmailInvalid(phone)) {  // 这里虽然传入的是phone，但是实际上是验证的邮箱
            return Result.fail("邮箱格式错误！");
        }
        // 验证码生成器
        String code = RandomUtil.randomNumbers(5);
        // 创建简单邮件信息
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(MyFrom);  // 发送人，用自己的邮件发
        message.setTo(phone);   // 谁要接受
        message.setSubject("验证码");  // 邮件标题
        message.setText("您的验证码是 \n" + code);  // 邮件内容

        try {
            // 发送邮件信息
            mailSender.send(message);
            // 需要保存一下验证码，后面用来验证
            // 保存到redis
            stringRedisTemplate.opsForValue().set(LOGIN_USER_KEY + phone, code, 2, TimeUnit.MINUTES);
            System.out.println("======");
            log.info(code + "======" + stringRedisTemplate.opsForValue().get(LOGIN_USER_KEY + phone));
            System.out.println("======");
        } catch (MailException e) {
            e.printStackTrace();
            return Result.fail("邮件发送失败");
        }
        return Result.ok();
    }

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    // 前端提交的数据是json格式，需要使用@RequestBody注解，将请求体转化成LoginFormDTO对象（忘了RequestBody注解的用法的话，翻看之前SpringMVC中第七章笔记的相关内容）
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        // 先验证邮箱邮箱
        String phone = loginForm.getPhone(); // 这里虽然传入的是phone，但是实际上是验证的邮箱
        if (RegexUtils.isEmailInvalid(phone)) {
            return Result.fail("邮箱格式错误！");
        }
        // 对验证码进行验证
        // 这里从redis中获取
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_USER_KEY + phone);
        // 从用户输入那里获取验证码，用于和存入redis中的验证码进行比较
        String code = loginForm.getCode();
        // 注意String类型的不能用==,!=来进行比较
        if (cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证码错误！");
        }

        // 查数据库存在此手机号（邮箱）？
        User user = userService.findByPhone(phone);
        // 不存在，创建新用户
        if (user == null) {
            // 存入数据库
            user = userService.creatUser(phone);
        }
        // 保存用户信息到redis中
        // 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();
        // 将User对象转为HashMap，存入redis
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) -> {
            return fieldValue.toString();
        }));

        // 存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + phone, map);
        // 设置token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, 30, TimeUnit.MINUTES);
        // 登录成功，删除验证码
        stringRedisTemplate.delete(LOGIN_USER_KEY + phone);
        // 返回token
        return Result.ok(token);
    }

    /**
     * 登出功能
     *
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout() {
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me() {
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
}
