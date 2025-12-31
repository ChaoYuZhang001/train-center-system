package com.train.service;

import com.train.util.Result;

import java.util.Map;

public interface AuthService {
    Result<Map<String, Object>> login(String account, String password, String orgId);
    /**
     * 用户退出
     * @param token JWT Token
     * @return 退出结果
     */
    Result<?> logout(String token);
}