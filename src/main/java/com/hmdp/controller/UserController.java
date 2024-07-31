package com.hmdp.controller;


import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
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
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        // 为什么需要session？因为后面我们需要把验证码保存在session中
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    // 前端提交的数据是json格式，需要使用@RequestBody注解，将请求体转化成LoginFormDTO对象（忘了RequestBody注解的用法的话，翻看之前SpringMVC中第七章笔记的相关内容）
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    /**
     * 登录的校验
     * @return
     */
    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
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
