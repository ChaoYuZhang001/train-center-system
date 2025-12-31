package com.train.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求DTO（支持机构区分）
 */
@Schema(description = "登录请求DTO（支持机构区分）")
@Data
public class LoginDTO {
    /**
     * 登录账号（可重复，需配合机构ID唯一）
     */
    @Schema(description = "登录账号", required = true, example = "admin")
    private String account;

    /**
     * 登录密码（明文）
     */
    @Schema(description = "登录密码", required = true, example = "123456")
    private String password;

    /**
     * 机构ID（关键：区分同账号不同机构）
     */
    @Schema(description = "机构ID", required = true, example = "ORG001")
    private String orgId;
}
