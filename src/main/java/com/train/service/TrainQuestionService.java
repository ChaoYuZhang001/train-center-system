package com.train.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.train.dto.TrainJudgeQuestionDTO;
import com.train.dto.TrainJudgeQuestionQuery;
import com.train.dto.TrainRandomQuestionOutDTO;
import com.train.entity.TrainQuestion;
import com.train.security.JwtUserDetails;
import com.train.util.Result;

public interface TrainQuestionService extends IService<TrainQuestion> {
    Result<TrainRandomQuestionOutDTO> selectRandomQuestions(String orgId, int randomNum);
    Result<TrainRandomQuestionOutDTO> reRandomQuestions(String paperId, String orgId, Integer randomNum);

    Result<TrainJudgeQuestionDTO> judge(JwtUserDetails orgId, TrainJudgeQuestionQuery trainJudgeQuestionQuery);

}
