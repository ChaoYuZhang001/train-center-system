package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("sys_role_menu")  // 对应数据库角色-菜单关联表
public class SysRoleMenu {
    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)  // 自增主键
    private Long id;              // 主键ID

    @Schema(description = "角色ID")
    private String roleId;        // 角色ID（关联sys_role表）

    @Schema(description = "菜单ID")
    private Long menuId;          // 菜单ID（关联sys_menu表）
}
