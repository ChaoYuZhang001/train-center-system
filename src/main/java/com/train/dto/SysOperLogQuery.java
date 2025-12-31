package com.train.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 操作日志查询参数
 */
@Data
@Schema(description = "操作日志查询参数")
public class SysOperLogQuery {

    @Schema(description = "操作ID 账号名")
    private Long operAccount;

    @Schema(description = "操作模块 菜单名")
    private String operMenu;

    @Schema(description = "操作类型 新增 编辑 删除 查询" )
    private String operType;

    @Schema(description = "操作开始时间")
    private String operTimeStart;

    @Schema(description = "操作结束时间")
    private String operTimeEnd;

    @Schema(description = "操作结果 成功 失败")
    private String operResult;
}
