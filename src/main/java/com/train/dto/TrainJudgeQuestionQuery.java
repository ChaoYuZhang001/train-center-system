package com.train.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "判断题查询参数")
public class TrainJudgeQuestionQuery implements Serializable {
    //生成试卷ID
    @Schema(description = "试卷ID")
    @NotBlank(message = "试卷ID不能为空")
    private String paperId;

    @Schema(description = "题目信息")
    private List<TrainJudgeQuestionQuery.TrainRandomQuestionInfo> trainRandomQuestionInfoList;

    @Data
    @Schema(description = "题目信息")
    public static class TrainRandomQuestionInfo {
        @Schema(description = "题目ID")
        @NotNull(message = "题目ID不能为空")
        private Long questionId;
        @Schema(description = "答案")
        @NotNull(message = "答案不能为空")
        private String answer;
    }
}
