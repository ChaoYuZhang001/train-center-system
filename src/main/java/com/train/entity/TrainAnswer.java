package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("train_answer")  // 对应数据库作答记录表
public class TrainAnswer {

    @Schema(description = "作答ID")
    @TableId(type = IdType.AUTO)  // 自增主键
    private Long answerId;         // 作答ID（主键）

    @Schema(description = "用户ID")
    private Long userId;           // 用户ID（关联sys_user表）

    @Schema(description = "试卷ID")
    private String paperId;        // 试卷ID（32位，关联缓存中的试卷）

    @Schema(description = "题目ID")
    private Long questionId;       // 题目ID（关联train_question表）

    @Schema(description = "用户答案")
    private String userAnswer;     // 用户答案（200位，支持多行输入）

    @Schema(description = "得分")
    private Integer score;         // 得分（0/20分）

    @Schema(description = "机构ID")
    private String orgId;
}
