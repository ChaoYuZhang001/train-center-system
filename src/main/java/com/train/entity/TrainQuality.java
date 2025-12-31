package com.train.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("train_quality")  // 对应数据库质量评价表
public class TrainQuality {
    /**
     * 评价表
     */
    @Schema(description = "评价ID")
    @TableId(type = IdType.AUTO)  // 自增主键
    private Long qualityId;        // 评价ID（主键）

    /**
     * 试卷ID（关联缓存中的试卷）
     */
    @Schema(description = "试卷ID")
    private String paperId;          // 试卷ID（32位，关联缓存中的试卷）

    /**
     * 用户ID（关联sys_user表）
     */
    @Schema(description = "用户ID")
    private Long userId;           // 用户ID（关联sys_user表）

    /**
     * 机构ID（关联sys_org表）
     */
    @Schema(description = "所属机构ID")
    private String orgId;          // 所属机构ID（关联sys_org表）

    /**
     * 考试得分（0/20/40/60/80/100，正确题数×20）
     */
    @Schema(description = "考试得分")
    private Integer score;         // 考试得分（0/20/40/60/80/100，正确题数×20）

    /**
     * 考试开始时间（格式：YYYY-MM-DD HH:MM:SS）
     */
    @Schema(description = "开始时间")
    private LocalDateTime startTime;  // 开始时间（格式：YYYY-MM-DD HH:MM:SS）

    /**
     * 考试结束时间（格式：YYYY-MM-DD HH:MM:SS）
     */
    @Schema(description = "结束时间")
    private LocalDateTime endTime;    // 结束时间（格式：YYYY-MM-DD HH:MM:SS）

    /**
     * 考试用时（格式：MM:ss）
     */
    @Schema(description = "答题用时")
    private String usedTime;        // 答题用时（格式：MM:ss）

    /**
     * 创建时间（格式：YYYY-MM-DD HH:MM:SS）
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;  // 创建时间
}
