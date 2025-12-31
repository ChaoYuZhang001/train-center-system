package com.train.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体类
 * 对应表：public.sys_role
 */
@Schema(description = "角色实体类")
@Data
@TableName("sys_role")
public class SysRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @Schema(description = "角色ID")
    private String roleId;

    /**
     * 角色名称（同一机构内唯一）
     */
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 角色描述
     */
    @Schema(description = "角色描述")
    private String roleDesc;

    /**
     * 角色类型（0=系统级，1=机构级）
     */
    @Schema(description = "角色类型", example = "0")
    private Integer roleType;

    /**
     * 状态（0=禁用，1=启用）
     */
    @Schema(description = "状态", example = "1")
    private Integer status;

    /**
     * 是否可删除（0=不可删除，1=可删除）
     */
    @Schema(description = "是否可删除", example = "1")
    private Integer canDelete;

    /**
     * 排序序号
     */
    @Schema(description = "排序序号")
    private Integer sort;

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
