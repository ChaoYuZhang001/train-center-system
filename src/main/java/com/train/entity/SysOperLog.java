package com.train.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.train.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Schema(description = "操作日志实体类")
@Data
@TableName("sys_oper_log")
public class SysOperLog {
    @Schema(description = "日志ID")
    private String logId;           // 日志ID

    @Schema(description = "机构ID")
    @TableField("oper_org_id")
    @Excel(name = "机构名称", sort = 1)
    private String operOrgId;           // 机构ID（对应数据库oper_org_id）

    @Schema(description = "操作账号")
    private String operAccount;     // 操作账号

    @Schema(description = "操作人姓名")
    private String operName;        // 操作人姓名

    @Schema(description = "操作菜单")
    @TableField("oper_menu")
    private String operMenu;        // 操作菜单

    @Schema(description = "操作类型")
    private String operType;        // 操作类型

    @Schema(description = "操作对象ID")
    private String targetId;       // 操作对象ID

    @Schema(description = "操作对象名称")
    private String targetName;     // 操作对象名称

    @Schema(description = "操作内容")
    private String operContent;     // 操作内容

    @Schema(description = "操作IP")
    private String operIp;          // 操作IP

    @Schema(description = "操作时间")
    private LocalDateTime operTime; // 操作时间

    @Schema(description = "操作结果")
    private String operResult;      // 操作结果
}
