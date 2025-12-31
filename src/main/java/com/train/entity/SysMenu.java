package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单实体类
 * 对应表：public.sys_menu
 * 支持树形结构（自关联parent_id）
 */
@Schema(description = "菜单实体类")
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID（自增主键）
     */
    @Schema(description = "菜单ID")
    @TableId(type = IdType.AUTO)
    private Long menuId;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称")
    private String menuName;

    /**
     * 父菜单ID（0=一级菜单）
     */
    @Schema(description = "父菜单ID")
    private Long parentId;

    /**
     * 菜单路径
     */
    @Schema(description = "菜单路径")
    private String path;

    /**
     * 权限标识（如sys:menu:list）
     */
    @Schema(description = "权限标识")
    private String perms;

    /**
     * 菜单类型（0=目录，1=菜单，2=按钮）
     */
    @Schema(description = "菜单类型", example = "0")
    private Integer type;

    /**
     * 排序序号
     */
    @Schema(description = "排序序号")
    private Integer sort;

    /**
     * 状态（0=禁用，1=启用）
     */
    @Schema(description = "状态", example = "1")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 子菜单列表（树形结构用，非数据库字段）
     */
    @Schema(description = "子菜单列表")
    @TableField(exist = false)
    private List<SysMenu> children;
}
