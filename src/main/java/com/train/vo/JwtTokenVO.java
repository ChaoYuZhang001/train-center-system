package com.train.vo;

import com.train.security.JwtUserDetails;
import lombok.Data;

/**
 * JWT登录响应VO
 */
@Data
public class JwtTokenVO {
    /**
     * 令牌类型（默认Bearer）
     */
    private String tokenType = "Bearer";

    /**
     * JWT令牌
     */
    private String accessToken;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户核心信息
     */
    private JwtUserDetails userInfo;
}