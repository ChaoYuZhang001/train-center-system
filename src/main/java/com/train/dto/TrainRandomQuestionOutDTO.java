package com.train.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 随机抽取的题目信息
 */
@Data
@Schema(description = "随机抽取的题目试卷")
public class TrainRandomQuestionOutDTO implements Serializable {

    //生成试卷ID
    @Schema(description = "试卷ID")
    private String paperId;

    @Schema(description = "开始时间 时间戳")
    private Long startTime;

    //随机抽取的题目信息
    @Schema(description = "随机抽取的题目信息")
    private List<TrainRandomQuestionInfo> trainRandomQuestionInfoList;
    @Data
    @Schema(description = "随机抽取的题目信息")
    public static class TrainRandomQuestionInfo {
        @Schema(description = "题目ID")
        private Long questionId;
        @Schema(description = "题目内容")
        private String questionContent;
        @Schema(description = "题目图片路径")
        private List<String> questionImg;
    }
}
