package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应表：public.sys_user
 */
@Schema(description = "用户实体类")
@Data
@TableName("sys_user")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（自增主键）
     */
    @Schema(description = "用户ID")
    @TableId(type = IdType.AUTO)
    private Long userId;

    /**
     * 登录账号（唯一）
     */
    @Schema(description = "登录账号")
    private String account;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String userName;

    /**
     * 登录密码（MD5加密）
     */
    @Schema(description = "登录密码")
    private String password;

    /**
     * 用户类型（0=超级管理员，1=普通用户）
     */
    @Schema(description = "用户类型", example = "1")
    private Integer type;

    /**
     * 状态（0=禁用，1=正常，2=离职，3=锁定）
     */
    @Schema(description = "状态", example = "1")
    private Integer status;

    /**
     * 所属机构ID
     */
    @Schema(description = "所属机构ID")
    private String orgId;

    /**
     * 是否系统超级管理员（0=否，1=是）
     */
    @Schema(description = "是否系统超级管理员", example = "0")
    private Integer isSysAdmin;

    /**
     * 是否可删除（0=不可删除，1=可删除）
     */
    @Schema(description = "是否可删除", example = "1")
    private Integer canDelete;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
