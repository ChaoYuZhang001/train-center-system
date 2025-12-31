package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 机构-角色关联实体类
 * 对应表：public.sys_org_role
 * 实现机构与角色的多对多关联
 */
@Schema(description = "机构-角色关联实体类")
@Data
@TableName("sys_org_role") //
public class SysOrgRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增主键）
     */
    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 机构ID（关联public.sys_org的org_id）
     */
    @Schema(description = "机构ID")
    private String orgId;

    /**
     * 角色ID（关联public.sys_role的role_id）
     */
    @Schema(description = "角色ID")
    private String roleId;

    /**
     * 创建时间（默认当前时间，无需手动赋值）
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
