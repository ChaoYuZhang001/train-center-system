package com.train.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.train.constant.Constants;
import com.train.constant.ResultConstant;
import com.train.dto.TrainJudgeQuestionDTO;
import com.train.dto.TrainJudgeQuestionQuery;
import com.train.dto.TrainRandomQuestionOutDTO;
import com.train.entity.TrainAnswer;
import com.train.entity.TrainQuality;
import com.train.entity.TrainQuestion;
import com.train.exception.BusinessException;
import com.train.mapper.TrainQualityMapper;
import com.train.mapper.TrainQuestionMapper;
import com.train.security.JwtUserDetails;
import com.train.service.TrainAnswerService;
import com.train.service.TrainQuestionService;
import com.train.util.RedisUtil;
import com.train.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 题库业务实现类
 * 负责随机抽题、答题判分、会话管理等核心业务
 */
@Service
public class TrainQuestionServiceImpl extends ServiceImpl<TrainQuestionMapper, TrainQuestion> implements TrainQuestionService {

    // ==================== 常量定义（消除魔法值，提高可维护性） ====================
    private static final Logger logger = LoggerFactory.getLogger(TrainQuestionServiceImpl.class);

    /** 答案分隔符（多标准答案分隔） */
    private static final String ANSWER_MULTI_SPLIT = "\\|\\|";
    /** 答案项分隔符（单题答案内部分隔） */
    private static final String ANSWER_ITEM_SPLIT = ",";
    /** 答题耗时格式化模板（mm:ss） */
    private static final String TIME_FORMAT_PATTERN = "%02d:%02d";
    /** 分布式锁过期时间（秒） */
    private static final long LOCK_EXPIRE_SECONDS = 30;

    // ==================== 配置注入 ====================
    @Value("${train.question.imageUrlPrefix}")
    private String imageUrlPrefix;

    @Value("${train.exam.session.expire.minutes:30}")
    private int sessionExpireMinutes;

    // ==================== 依赖注入 ====================
    @Resource
    private TrainAnswerService trainAnswerService;
    @Resource
    private TrainQualityMapper trainQualityMapper;
    @Resource
    private RedisUtil redisUtil;

    // ==================== 核心业务方法：随机抽题 ====================
    /**
     * 按机构随机抽取不重复题目
     *
     * @param orgId     机构ID
     * @param randomNum 抽题数量
     * @return 抽题结果（含题目信息、会话ID）
     */
    @Override
    public Result<TrainRandomQuestionOutDTO> selectRandomQuestions(String orgId, int randomNum) {
        // 1. 构造机构ID列表（系统机构+目标机构）
        List<String> orgIdList = new ArrayList<>(2);
        orgIdList.add(Constants.SYS_ORG_ID);
        if (!Constants.SYS_ORG_ID.equals(orgId)) {
            orgIdList.add(orgId);
        }

        // 2. 数据库随机抽题（使用baseMapper，移除冗余注入）
        List<TrainQuestion> questionList = this.baseMapper.selectRandomQuestions(orgIdList, randomNum);
        if (CollectionUtils.isEmpty(questionList)) {
            logger.info("机构{}未查询到可用题目，抽题数量{}", orgId, randomNum);
            return Result.success(null);
        }

        // 3. 转换为输出DTO
        TrainRandomQuestionOutDTO randomQuestionOutDTO = buildRandomQuestionOutDTO(questionList);

        // 4. 生成唯一会话ID
        String paperId = UUID.randomUUID().toString().replace("-", "");
        randomQuestionOutDTO.setPaperId(paperId);
        randomQuestionOutDTO.setStartTime(System.currentTimeMillis());

        // 5. Redis会话存储（异常隔离，不影响主流程）
        saveExamSessionToRedis(orgId, paperId, randomQuestionOutDTO);

        return Result.success(randomQuestionOutDTO);
    }
    // ==================== 核心业务方法：重新随机抽题 ====================
    /**
     * 重新按机构随机抽取不重复题目（保证新题目与旧题目不一致，清理旧会话，生成新会话ID）
     *
     * @param paperId   旧的试卷/会话ID
     * @param orgId     机构ID
     * @param randomNum 抽题数量
     * @return 新的抽题结果（含新会话ID、与旧题无重复的新题目信息）
     */
    @Override
    public Result<TrainRandomQuestionOutDTO> reRandomQuestions(String paperId, String orgId, Integer randomNum) {
        // 1. 前置参数校验（保持严谨性，与原有逻辑一致）
        if (!StringUtils.hasText(paperId)) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "旧试卷ID（会话ID）不能为空");
        }
        if (!StringUtils.hasText(orgId)) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "机构ID不能为空且不能为空白字符");
        }
        if (randomNum == null || randomNum <= 0) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "抽题数量必须大于0");
        }

        // 2. 构建旧会话Redis Key，获取旧会话中的题目ID（核心：用于后续去重）
        String oldSessionKey = buildExamSessionKey(orgId, paperId);
        List<Long> oldQuestionIds = new ArrayList<>();
        try {
            // 先校验旧会话是否存在
            boolean oldSessionExists = redisUtil.exists(oldSessionKey);
            if (oldSessionExists) {
                // 获取旧会话完整信息
                TrainRandomQuestionOutDTO oldSessionInfo = (TrainRandomQuestionOutDTO) redisUtil.get(oldSessionKey);
                if (oldSessionInfo != null && !CollectionUtils.isEmpty(oldSessionInfo.getTrainRandomQuestionInfoList())) {
                    // 提取旧会话中的所有题目ID，用于后续排除
                    oldQuestionIds = oldSessionInfo.getTrainRandomQuestionInfoList().stream()
                            .map(TrainRandomQuestionOutDTO.TrainRandomQuestionInfo::getQuestionId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    logger.info("成功获取旧会话题目ID，共{}道题，旧paperId：{}", oldQuestionIds.size(), paperId);
                }

                // 删除旧会话
                boolean deleteSuccess = redisUtil.delete(oldSessionKey);
                if (deleteSuccess) {
                    logger.info("旧答题会话删除成功，旧paperId：{}，机构ID：{}", paperId, orgId);
                } else {
                    logger.warn("旧答题会话删除失败，旧paperId：{}，机构ID：{}", paperId, orgId);
                }
            } else {
                logger.info("未查询到旧答题会话，无需删除，旧paperId：{}，机构ID：{}", paperId, orgId);
            }
        } catch (Exception e) {
            // Redis操作异常不中断主流程，仅记录日志
            logger.error("校验/删除旧答题会话、提取旧题目ID异常，旧paperId：{}，机构ID：{}", paperId, orgId, e);
        }

        // 3. 构造机构ID列表（与原抽题逻辑一致：系统机构+目标机构）
        List<String> orgIdList = new ArrayList<>(2);
        orgIdList.add(Constants.SYS_ORG_ID);
        if (!Constants.SYS_ORG_ID.equals(orgId)) {
            orgIdList.add(orgId);
        }

        // 4. 数据库重新随机抽题（排除旧题目ID，保证新题与旧题不一致）
        List<TrainQuestion> questionList;
        try {
            // 调用Mapper方法：传入机构ID列表 + 排除的旧题目ID + 抽题数量，实现去重抽题
            // 【注意】：需要你在 TrainQuestionMapper 中新增对应的查询方法（下方提供Mapper方法示例）
            questionList = this.baseMapper.selectRandomQuestionsExcludeOldIds(orgIdList, oldQuestionIds, randomNum);
        } catch (Exception e) {
            logger.error("重新抽题（排除旧题目）失败，机构ID：{}，排除题目数：{}，抽题数量：{}", orgId, oldQuestionIds.size(), randomNum, e);
            throw new BusinessException(ResultConstant.ERROR_CODE, "重新抽题失败，请稍后重试");
        }

        // 校验抽题结果
        if (CollectionUtils.isEmpty(questionList)) {
            logger.info("机构{}重新抽题（排除{}道旧题）未查询到可用题目，抽题数量{}", orgId, oldQuestionIds.size(), randomNum);
            return Result.success(null);
        }

        // 5. 转换为输出DTO（复用已有辅助方法，保持代码一致性）
        TrainRandomQuestionOutDTO randomQuestionOutDTO = buildRandomQuestionOutDTO(questionList);

        // 6. 生成新的唯一paperId（保证与旧paperId不一致，双重保障）
        String newPaperId;
        do {
            newPaperId = UUID.randomUUID().toString().replace("-", "");
        } while (paperId.equals(newPaperId)); // 确保新ID与旧ID绝对不一致

        // 7. 设置新会话信息
        randomQuestionOutDTO.setPaperId(newPaperId);
        randomQuestionOutDTO.setStartTime(System.currentTimeMillis());

        // 8. 存储新的答题会话到Redis（复用已有辅助方法）
        saveExamSessionToRedis(orgId, newPaperId, randomQuestionOutDTO);

        logger.info("机构{}重新抽题成功（新题与旧题无重复），旧paperId：{}，新paperId：{}，抽题数量{}",
                orgId, paperId, newPaperId, randomNum);
        return Result.success(randomQuestionOutDTO);
    }

    // ==================== 核心业务方法：答题判分 ====================
    /**
     * 答题提交与自动判分
     * 包含空答案校验、重复提交控制、得分计算、数据入库
     *
     * @param userDetails        当前登录用户信息
     * @param trainJudgeQuestionQuery 答题提交参数
     * @return 判分结果（含每题对错、总分）
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 异常全回滚，保证事务一致性
    public Result<TrainJudgeQuestionDTO> judge(JwtUserDetails userDetails, TrainJudgeQuestionQuery trainJudgeQuestionQuery) {
        // 1. 前置参数校验
        validateJudgeParam(trainJudgeQuestionQuery);

        Long userId = userDetails.getUserId();
        String orgId = userDetails.getOrgId();
        String paperId = trainJudgeQuestionQuery.getPaperId();
        List<TrainJudgeQuestionQuery.TrainRandomQuestionInfo> userAnswerList = trainJudgeQuestionQuery.getTrainRandomQuestionInfoList();

        // 2. 构建返回DTO
        TrainJudgeQuestionDTO judgeResultDTO = new TrainJudgeQuestionDTO();
        judgeResultDTO.setPaperId(paperId);
        List<TrainJudgeQuestionDTO.TrainRandomQuestionInfo> questionResultList = new ArrayList<>(userAnswerList.size());

        // 3. 获取Redis会话信息
        String sessionKey = buildExamSessionKey(orgId, paperId);
        TrainRandomQuestionOutDTO sessionInfo = getExamSessionFromRedis(sessionKey);
        if (Objects.isNull(sessionInfo)) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "答题会话已过期或不存在");
        }
        // 【题目ID一致性校验】
        validateQuestionIdConsistency(sessionInfo, userAnswerList);

        // 4. 空答案校验（修正原逻辑漏洞）
        if (hasEmptyAnswer(userAnswerList)) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "存在未填写的答案，请补全后提交");
        }

        // 5. 分布式锁控制重复提交
        String lockKey = buildJudgeLockKey(paperId, userId);
        String lockValue = UUID.randomUUID().toString().replace("-", "");
        boolean lockAcquired = false;
        boolean sessionCleaned = false;

        try {
            // 获取分布式锁
            lockAcquired = redisUtil.setIfAbsent(lockKey, lockValue, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new BusinessException(ResultConstant.ERROR_CODE, "请勿重复提交答案");
            }

            // 6. 批量处理答案判分
            TrainQuality trainQuality = buildTrainQualityBaseInfo(userDetails, paperId, sessionInfo);
            List<TrainAnswer> answersToInsert = batchJudgeAnswers(userAnswerList, userId, orgId, paperId, questionResultList, trainQuality);

            // 7. 批量入库（提高性能）
            if (!CollectionUtils.isEmpty(answersToInsert)) {
                trainAnswerService.saveBatch(answersToInsert);
            }
            // 保存答题质量信息
            trainQualityMapper.insert(trainQuality);

            // 8. 设置返回结果
            judgeResultDTO.setTrainRandomQuestionInfoList(questionResultList);
            judgeResultDTO.setScore(trainQuality.getScore());

        } finally {
            // 释放分布式锁（确保最终执行）
            if (lockAcquired) {
                releaseDistributedLock(lockKey, lockValue);
            }
            // 清理Redis会话（确保最终执行）
            if (!sessionCleaned) {
                cleanExamSessionFromRedis(sessionKey);
            }
        }

        return Result.success(judgeResultDTO);
    }

    // ==================== 私有辅助方法：职责单一，提高可读性 ====================

    /**
     * 构建随机抽题输出DTO
     *
     * @param questionList 原始题目列表
     * @return 格式化后的输出DTO
     */
    private TrainRandomQuestionOutDTO buildRandomQuestionOutDTO(List<TrainQuestion> questionList) {
        TrainRandomQuestionOutDTO outDTO = new TrainRandomQuestionOutDTO();
        List<TrainRandomQuestionOutDTO.TrainRandomQuestionInfo> infoList = questionList.stream()
                .map(this::convertToRandomQuestionInfo)
                .filter(Objects::nonNull) // 过滤空对象，避免空指针
                .collect(Collectors.toList());
        outDTO.setTrainRandomQuestionInfoList(infoList);
        return outDTO;
    }

    /**
     * 转换题目信息为抽题结果详情
     *
     * @param question 原始题目实体
     * @return 抽题结果详情
     */
    private TrainRandomQuestionOutDTO.TrainRandomQuestionInfo convertToRandomQuestionInfo(TrainQuestion question) {
        if (Objects.isNull(question)) {
            return null;
        }
        TrainRandomQuestionOutDTO.TrainRandomQuestionInfo info = new TrainRandomQuestionOutDTO.TrainRandomQuestionInfo();
        info.setQuestionId(question.getQuestionId());
        info.setQuestionContent(question.getQuestionContent());
        // 处理题目图片前缀
        info.setQuestionImg(processQuestionImages(question.getQuestionImg()));
        return info;
    }

    /**
     * 处理题目图片，拼接前缀URL
     *
     * @param questionImg 原始图片字符串（逗号分隔）
     * @return 格式化后的图片URL列表
     */
    private List<String> processQuestionImages(String questionImg) {
        if (!StringUtils.hasText(questionImg)) {
            return Collections.emptyList(); // 返回空集合，避免null
        }
        try {
            return Arrays.stream(questionImg.split(ANSWER_ITEM_SPLIT))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(img -> imageUrlPrefix + img)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("处理题目图片失败，原始图片字符串：{}", questionImg, e);
            return Collections.emptyList();
        }
    }

    /**
     * 保存答题会话到Redis
     *
     * @param orgId          机构ID
     * @param paperId        会话ID（试卷ID）
     * @param sessionContent 会话内容
     */
    private void saveExamSessionToRedis(String orgId, String paperId, TrainRandomQuestionOutDTO sessionContent) {
        try {
            String sessionKey = buildExamSessionKey(orgId, paperId);
            redisUtil.set(sessionKey, sessionContent, sessionExpireMinutes, TimeUnit.MINUTES);
            logger.info("答题会话保存成功，key：{}，过期时间：{}分钟", sessionKey, sessionExpireMinutes);
        } catch (Exception e) {
            logger.error("答题会话保存失败，机构ID：{}，会话ID：{}", orgId, paperId, e);
            // 异常不中断主流程，仅日志记录
        }
    }

    /**
     * 从Redis获取答题会话
     *
     * @param sessionKey Redis key
     * @return 会话信息
     */
    private TrainRandomQuestionOutDTO getExamSessionFromRedis(String sessionKey) {
        try {
            return (TrainRandomQuestionOutDTO) redisUtil.get(sessionKey);
        } catch (Exception e) {
            logger.error("获取答题会话失败，key：{}", sessionKey, e);
            return null;
        }
    }

    /**
     * 清理Redis中的答题会话
     *
     * @param sessionKey Redis key
     */
    private void cleanExamSessionFromRedis(String sessionKey) {
        try {
            redisUtil.delete(sessionKey);
            logger.info("答题会话清理成功，key：{}", sessionKey);
        } catch (Exception e) {
            logger.error("答题会话清理失败，key：{}", sessionKey, e);
        }
    }

    /**
     * 构建答题会话Redis Key
     *
     * @param orgId   机构ID
     * @param paperId 会话ID
     * @return 安全的Redis Key
     */
    private String buildExamSessionKey(String orgId, String paperId) {
        String safeOrgId = sanitizeRedisKey(orgId);
        String safePaperId = sanitizeRedisKey(paperId);
        return Constants.EXAM_SESSION_KEY + safeOrgId + ":" + safePaperId;
    }

    /**
     * 构建答题判分分布式锁Key
     *
     * @param paperId 会话ID
     * @param userId  用户ID
     * @return 分布式锁Key
     */
    private String buildJudgeLockKey(String paperId, Long userId) {
        String safePaperId = sanitizeRedisKey(paperId);
        String safeUserId = sanitizeRedisKey(userId.toString());
        return "exam_lock:" + safePaperId + ":" + safeUserId;
    }

    /**
     * 安全清理Redis Key，过滤特殊字符
     *
     * @param key 原始Key
     * @return 安全的Key（仅保留字母、数字、下划线）
     */
    private String sanitizeRedisKey(String key) {
        if (!StringUtils.hasText(key)) {
            return "_empty_"; // 避免空字符串作为Key部分
        }
        return key.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * 校验答题判分前置参数
     *
     * @param query       提交参数
     */
    private void validateJudgeParam(TrainJudgeQuestionQuery query) {
        if (Objects.isNull(query) || !StringUtils.hasText(query.getPaperId())) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "答题会话ID（试卷ID）不能为空");
        }
        if (CollectionUtils.isEmpty(query.getTrainRandomQuestionInfoList())) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "答题内容不能为空");
        }
    }

    /**
     * 校验用户提交的题目ID与Redis会话中的题目ID是否一致
     * 1. 校验题目数量是否匹配
     * 2. 校验提交的题目ID是否全部在会话题目ID中（无多余、无缺失）
     *
     * @param sessionInfo    Redis中的答题会话信息
     * @param userAnswerList 用户提交的答题列表
     */
    private void validateQuestionIdConsistency(TrainRandomQuestionOutDTO sessionInfo,
                                               List<TrainJudgeQuestionQuery.TrainRandomQuestionInfo> userAnswerList) {
        // 1. 提取Redis会话中的题目ID（转为Set，提高查询效率）
        List<TrainRandomQuestionOutDTO.TrainRandomQuestionInfo> sessionQuestionList = sessionInfo.getTrainRandomQuestionInfoList();
        if (CollectionUtils.isEmpty(sessionQuestionList)) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "本次会话无可用题目，禁止提交");
        }
        Set<Long> sessionQuestionIdSet = sessionQuestionList.stream()
                .map(TrainRandomQuestionOutDTO.TrainRandomQuestionInfo::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 提取用户提交的题目ID（转为Set，去重+高效查询）
        if (CollectionUtils.isEmpty(userAnswerList)) {
            throw new BusinessException(ResultConstant.ERROR_CODE, "提交的答题内容不能为空");
        }
        Set<Long> submitQuestionIdSet = userAnswerList.stream()
                .map(TrainJudgeQuestionQuery.TrainRandomQuestionInfo::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 第一步校验：题目数量是否一致（避免少提交、多提交题目）
        if (sessionQuestionIdSet.size() != submitQuestionIdSet.size()) {
            logger.error("题目数量不匹配，会话题目数：{}，提交题目数：{}", sessionQuestionIdSet.size(), submitQuestionIdSet.size());
            throw new BusinessException(ResultConstant.ERROR_CODE, String.format("提交的题目数量与本次会话不一致（应提交%d题，实际提交%d题）",
                    sessionQuestionIdSet.size(), submitQuestionIdSet.size()));
        }

        // 4. 第二步校验：提交的题目ID是否全部在会话题目ID中（无非法题目ID）
        for (Long submitQuestionId : submitQuestionIdSet) {
            if (!sessionQuestionIdSet.contains(submitQuestionId)) {
                logger.error("提交非法题目ID，非本次会话题目：{}", submitQuestionId);
                throw new BusinessException(ResultConstant.ERROR_CODE, "提交了非本次会话的题目ID，禁止提交");
            }
        }

        logger.info("题目ID一致性校验通过，会话题目数：{}，提交题目数：{}", sessionQuestionIdSet.size(), submitQuestionIdSet.size());
    }
    /**
     * 检查是否存在空答案
     * 【修正原逻辑漏洞】：原代码使用noneMatch导致逻辑颠倒
     *
     * @param answerList 用户答题列表
     * @return true-存在空答案，false-无空答案
     */
    private boolean hasEmptyAnswer(List<TrainJudgeQuestionQuery.TrainRandomQuestionInfo> answerList) {
        return answerList.stream()
                .anyMatch(answer -> !StringUtils.hasText(answer.getAnswer()));
    }

    /**
     * 构建答题质量基础信息
     *
     * @param userDetails 用户信息
     * @param paperId     会话ID
     * @param sessionInfo 会话信息
     * @return 答题质量基础实体
     */
    private TrainQuality buildTrainQualityBaseInfo(JwtUserDetails userDetails, String paperId, TrainRandomQuestionOutDTO sessionInfo) {
        TrainQuality trainQuality = new TrainQuality();
        trainQuality.setPaperId(paperId);
        trainQuality.setUserId(userDetails.getUserId());
        trainQuality.setOrgId(userDetails.getOrgId());
        trainQuality.setEndTime(LocalDateTime.now());
        // 转换开始时间戳为LocalDateTime
        LocalDateTime startTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(sessionInfo.getStartTime()),
                ZoneId.systemDefault()
        );
        trainQuality.setStartTime(startTime);
        // 初始化得分
        trainQuality.setScore(0);
        return trainQuality;
    }

    /**
     * 批量处理答案判分
     *
     * @param userAnswerList   用户答题列表
     * @param userId           用户ID
     * @param orgId            机构ID
     * @param paperId          会话ID
     * @param questionResultList 题目结果列表
     * @param trainQuality     答题质量实体
     * @return 待入库的答案列表
     */
    private List<TrainAnswer> batchJudgeAnswers(List<TrainJudgeQuestionQuery.TrainRandomQuestionInfo> userAnswerList,
                                                Long userId, String orgId, String paperId,
                                                List<TrainJudgeQuestionDTO.TrainRandomQuestionInfo> questionResultList,
                                                TrainQuality trainQuality) {
        List<TrainAnswer> answersToInsert = new ArrayList<>(userAnswerList.size());

        for (TrainJudgeQuestionQuery.TrainRandomQuestionInfo userAnswer : userAnswerList) {
            // 构建答案实体
            TrainAnswer trainAnswer = buildTrainAnswerBaseInfo(userAnswer, userId, orgId, paperId);
            // 构建题目结果DTO
            TrainJudgeQuestionDTO.TrainRandomQuestionInfo resultInfo = convertToJudgeResultInfo(userAnswer);
            // 查询标准答案
            TrainQuestion question = this.baseMapper.selectAnswer(userAnswer.getQuestionId());
            if (Objects.isNull(question)) {
                logger.error("题目不存在，题目ID：{}", userAnswer.getQuestionId());
                resultInfo.setIsRight(false);
                questionResultList.add(resultInfo);
                continue;
            }
            // 单题判分
            boolean isCorrect = judgeSingleQuestion(question.getAnswer(), userAnswer.getAnswer(), trainAnswer);
            resultInfo.setIsRight(isCorrect);
            // 累加总分
            trainQuality.setScore(trainQuality.getScore() + trainAnswer.getScore());
            // 添加结果
            questionResultList.add(resultInfo);
            answersToInsert.add(trainAnswer);
        }

        // 计算答题耗时
        calculateUsedTime(trainQuality, userAnswerList.get(0), userAnswerList.get(userAnswerList.size() - 1));

        return answersToInsert;
    }

    /**
     * 构建答案基础信息
     *
     * @param userAnswer 用户答题信息
     * @param userId     用户ID
     * @param orgId      机构ID
     * @param paperId    会话ID
     * @return 答案基础实体
     */
    private TrainAnswer buildTrainAnswerBaseInfo(TrainJudgeQuestionQuery.TrainRandomQuestionInfo userAnswer,
                                                 Long userId, String orgId, String paperId) {
        TrainAnswer trainAnswer = new TrainAnswer();
        trainAnswer.setPaperId(paperId);
        trainAnswer.setOrgId(orgId);
        trainAnswer.setQuestionId(userAnswer.getQuestionId());
        trainAnswer.setUserAnswer(userAnswer.getAnswer());
        trainAnswer.setUserId(userId);
        trainAnswer.setScore(0); // 初始化得分
        return trainAnswer;
    }

    /**
     * 转换用户答题信息为判分结果信息
     *
     * @param userAnswer 用户答题信息
     * @return 判分结果信息
     */
    private TrainJudgeQuestionDTO.TrainRandomQuestionInfo convertToJudgeResultInfo(TrainJudgeQuestionQuery.TrainRandomQuestionInfo userAnswer) {
        TrainJudgeQuestionDTO.TrainRandomQuestionInfo resultInfo = new TrainJudgeQuestionDTO.TrainRandomQuestionInfo();
        BeanUtils.copyProperties(userAnswer, resultInfo);
        return resultInfo;
    }

    /**
     * 单题判分（完全匹配规则）
     *
     * @param standardAnswer 标准答案
     * @param userAnswer     用户答案
     * @param trainAnswer    答案实体（用于设置得分）
     * @return true-答对，false-答错
     */
    private boolean judgeSingleQuestion(String standardAnswer, String userAnswer, TrainAnswer trainAnswer) {
        // 标准答案为空，直接判定错误
        if (!StringUtils.hasText(standardAnswer)) {
            return false;
        }

        String[] standardAnswerGroups = standardAnswer.split(ANSWER_MULTI_SPLIT);
        String submitAnswer = userAnswer.trim();

        // 遍历多标准答案组，任一匹配即算正确
        for (String standardGroup : standardAnswerGroups) {
            if (isAnswerGroupMatch(standardGroup, submitAnswer)) {
                trainAnswer.setScore(20); // 每题20分
                return true;
            }
        }

        trainAnswer.setScore(0);
        return false;
    }

    /**
     * 校验单组答案是否完全匹配
     *
     * @param standardGroup 单组标准答案
     * @param submitAnswer  用户提交答案
     * @return true-匹配，false-不匹配
     */
    private boolean isAnswerGroupMatch(String standardGroup, String submitAnswer) {
        String[] expectedParts = standardGroup.split(ANSWER_ITEM_SPLIT);
        String[] userParts = submitAnswer.split(ANSWER_ITEM_SPLIT);

        // 遍历标准答案项，全部匹配才算合格
        for (String expectedPart : expectedParts) {
            boolean partMatched = false;
            for (String userPart : userParts) {
                if (expectedPart.trim().equals(userPart.trim())) {
                    partMatched = true;
                    break;
                }
            }
            if (!partMatched) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算答题耗时（格式化为mm:ss）
     * 【优化】：使用Duration类，更优雅的时间计算
     *
     * @param trainQuality 答题质量实体
     * @param firstAnswer  首题答题信息
     * @param lastAnswer   末题答题信息
     */
    private void calculateUsedTime(TrainQuality trainQuality,
                                   TrainJudgeQuestionQuery.TrainRandomQuestionInfo firstAnswer,
                                   TrainJudgeQuestionQuery.TrainRandomQuestionInfo lastAnswer) {
        // 从会话中获取开始时间，计算与结束时间的差值
        LocalDateTime startTime = trainQuality.getStartTime();
        LocalDateTime endTime = trainQuality.getEndTime();
        Duration duration = Duration.between(startTime, endTime);

        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;

        String usedTime = String.format(TIME_FORMAT_PATTERN, minutes, seconds);
        trainQuality.setUsedTime(usedTime);
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey   锁Key
     * @param lockValue 锁Value（确保原子释放）
     */
    private void releaseDistributedLock(String lockKey, String lockValue) {
        try {
            redisUtil.deleteIfEquals(lockKey, lockValue);
            logger.info("分布式锁释放成功，key：{}", lockKey);
        } catch (Exception e) {
            logger.error("分布式锁释放失败，key：{}", lockKey, e);
        }
    }
}
