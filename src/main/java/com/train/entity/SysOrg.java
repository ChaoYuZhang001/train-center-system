package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 机构实体类
 * 对应表：public.sys_org
 */
@Schema(description = "机构实体类")
@Data
@TableName("sys_org")
public class SysOrg implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 机构ID（年月日+3位序号）
     */
    @Schema(description = "机构ID")
    @TableId(type = IdType.INPUT) // 手动输入ID，非自增
    private String orgId;

    /**
     * 机构名称（唯一）
     */
    @Schema(description = "机构名称")
    private String orgName;

    /**
     * 机构管理员账号
     */
    @Schema(description = "机构管理员账号")
    private String adminAccount;

    /**
     * 机构管理员姓名
     */
    @Schema(description = "机构管理员姓名")
    private String adminName;

    /**
     * 机构管理员密码（MD5加密）
     */
    @Schema(description = "机构管理员密码")
    private String adminPassword;

    /**
     * 状态（0=禁用，1=启用）
     */
    @Schema(description = "状态", example = "1")
    private Integer status;

    /**
     * 默认机构管理员用户ID
     */
    @Schema(description = "默认机构管理员用户ID")
    private Long defaultAdminUserId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
