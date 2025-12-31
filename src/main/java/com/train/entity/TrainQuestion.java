package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 题库实体类
 * 用于映射数据库中的train_question表，包含题目的基本信息
 */
@Schema(description = "题库实体类")
@Data
@TableName("train_question")
public class TrainQuestion {

    /**
     * 题目ID
     */
    @Schema(description = "题目ID")
    @TableId(type = IdType.AUTO)
    private Long questionId;       // 题目ID（主键）

    /**
     * 题目内容
     */
    @Schema(description = "题目内容")
    private String questionContent;  // 题目内容（500位）

    /**
     * 题目图片
     */
    @Schema(description = "题目图片路径")
    private String questionImg;    // 题目图片路径（255位，可选）

    /**
     * 答案
     */
    @Schema(description = "正确答案")
    private String answer;         // 正确答案（200位）

    /**
     * 状态
     */
    @Schema(description = "状态", example = "1")
    private Integer status;        // 状态（0=禁用，1=启用，默认1）

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;  // 创建时间
}
