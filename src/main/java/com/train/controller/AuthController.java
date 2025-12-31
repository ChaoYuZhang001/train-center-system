package com.train.controller;

import com.train.annotation.IgnoreLog;
import com.train.service.AuthService;
import com.train.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/train/auth")
@Tag(name = "认证管理", description = "用户登录、退出接口")
public class AuthController {
    @Resource
    private AuthService authService;

    @Operation(summary = "用户登录(密码AES对称加密)", description = "传入账号、密码和机构ID，认证通过后返回JWT Token")
    @PostMapping("/login")
    @IgnoreLog
    public Result<Map<String, Object>> login(@RequestParam String account,
                                             @RequestParam(name = "password",  defaultValue = "h704/1LvkG555m+12IZcMLfgl6FqcJlRKvyWQWr3yvA=") String password,
                                             @RequestParam(name = "机构ID,(超级管理员可不传使用系统默认值)", required = false) String orgId) {
        return authService.login(account, password, orgId);
    }

    /**
     * 用户退出接口
     * @param authorization 请求头（Bearer Token）
     * @return 退出结果
     */
    @Operation(summary = "用户退出", description = "传入JWT Token，将Token加入黑名单，使其失效")
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("Authorization") String authorization) {
        // 截取Bearer后的Token字符串
        String token = authorization.substring(7);
        return authService.logout(token);
    }
}