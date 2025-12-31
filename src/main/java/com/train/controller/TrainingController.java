package com.train.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.annotation.RequiresPermission;
import com.train.constant.Constants;
import com.train.dto.TrainJudgeQuestionDTO;
import com.train.dto.TrainJudgeQuestionQuery;
import com.train.dto.TrainRandomQuestionOutDTO;
import com.train.entity.TrainVideo;
import com.train.security.JwtUserDetails;
import com.train.service.TrainQuestionService;
import com.train.service.TrainVideoService;
import com.train.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "培训中心", description = "培训中心 -包含 视频培训 在线练习 多中心质量控制")
@RestController
@RequestMapping("/train/training")
public class TrainingController {

    @Autowired
    private TrainVideoService trainVideoService;

    @Autowired
    private TrainQuestionService trainQuestionService;

    /**
     * 分页查询视频列表
     * @param page 页码
     * @param size 每页大小
     * @return 视频分页列表
     */
    @GetMapping("/video/list")
    @Operation(summary = "分页查询视频列表", description = "分页查询视频列表")
    @RequiresPermission("train:video")
    public Result<IPage<TrainVideo>> getVideoList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size){
        // 获取当前登录用户
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String orgId = userDetails.getOrgId();
        Page<TrainVideo> pageInfo = new Page<>(page, size);
        IPage<TrainVideo> result = trainVideoService.getVideoList(pageInfo, orgId);
        return Result.success(result);
    }

    /**
     * 根据ID查询视频详情
     * @param videoId 视频ID
     * @return 视频详情
     */
    @GetMapping("/video/{videoId}")
    @Operation(summary = "根据ID查询视频详情", description = "根据视频ID查询视频详情")
    @RequiresPermission("train:video")
    public Result<TrainVideo> getVideoById(@PathVariable Long videoId) {
        TrainVideo video = trainVideoService.getById(videoId);
        if (video != null && video.getStatus() != 0) {
            return Result.success(video);
        }
        return Result.error("视频不存在或已被禁用");
    }



    @GetMapping("/question/randomQuestions")
    @Operation(summary = "随机抽取题目", description = "随机抽取题目")
    @RequiresPermission("train:practice")
    public Result<TrainRandomQuestionOutDTO> randomQuestions() {
        // 获取当前登录用户
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String orgId = userDetails.getOrgId();
        // 1. 随机抽取5道题（SQL：ORDER BY RANDOM () LIMIT 5）
        return trainQuestionService.selectRandomQuestions(orgId, Constants.FIVE);
    }
    //重新抽题
    @GetMapping("/question/reRandomQuestions")
    @Operation(summary = "重新抽题", description = "重新抽题")
    @RequiresPermission("train:practice")
    public Result<TrainRandomQuestionOutDTO> reRandomQuestions( @RequestParam(defaultValue = "1") String paperId) {
        // 获取当前登录用户
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String orgId = userDetails.getOrgId();
        // 1. 随机抽取5道题（SQL：ORDER BY RANDOM () LIMIT 5）
        return trainQuestionService.reRandomQuestions(paperId, orgId, Constants.FIVE);
    }

    @PostMapping("/question/judge")
    @Operation(summary = "答题提交与校验", description = "答题提交与校验")
    @RequiresPermission("train:practice")
    public Result<TrainJudgeQuestionDTO> judge(@RequestBody TrainJudgeQuestionQuery trainJudgeQuestionQuery) {
        // 获取当前登录用户
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return trainQuestionService.judge(userDetails,trainJudgeQuestionQuery);
    }
}
