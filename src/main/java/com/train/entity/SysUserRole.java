package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("sys_user_role")  // 对应数据库用户-角色关联表
public class SysUserRole {
    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)  // 自增主键
    private Long id;              // 主键ID

    @Schema(description = "用户ID")
    private Long userId;          // 用户ID（关联sys_user表）

    @Schema(description = "角色ID")
    private String roleId;        // 角色ID（关联sys_role表）
}
