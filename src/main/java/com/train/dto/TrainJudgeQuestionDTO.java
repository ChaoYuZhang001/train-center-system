package com.train.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "判断题信息")
public class TrainJudgeQuestionDTO {
    @Schema(description = "试卷ID")
    private String paperId;
    //得分
    @Schema(description = "得分")
    private Integer score;
    @Schema(description = "题目信息")
    private List<TrainJudgeQuestionDTO.TrainRandomQuestionInfo> trainRandomQuestionInfoList;

    @Data
    @Schema(description = "题目信息")
    public static class TrainRandomQuestionInfo {
        @Schema(description = "题目ID")
        private Long questionId;
        @Schema(description = "答案")
        private String answer;
        @Schema(description = "是否正确")
        private Boolean isRight;
    }
}
