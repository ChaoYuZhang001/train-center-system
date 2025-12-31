package com.train.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.train.entity.TrainQuestion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TrainQuestionMapper extends BaseMapper<TrainQuestion> {
    // 随机抽取5道题（用于生成试卷）
    List<TrainQuestion> selectRandomQuestions(@Param("orgIds") List<String> orgIds, @Param("limit") Integer limit);

    // 根据题目ID查询答案和关键词（用于判分）
    TrainQuestion selectAnswer(@Param("questionId") Long questionId);

    /**
     * 按机构随机抽题（排除指定旧题目ID）
     * @param orgIds  机构ID列表
     * @param excludeQuestionIds  需要排除的旧题目ID列表
     * @param limit  抽题数量
     * @return  无重复的新题目列表
     */
    List<TrainQuestion> selectRandomQuestionsExcludeOldIds(
            @Param("orgIds") List<String> orgIds,
            @Param("excludeQuestionIds") List<Long> excludeQuestionIds,
            @Param("limit") int limit
    );}