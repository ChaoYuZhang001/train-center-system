package com.train.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.entity.TrainQuality;
import org.apache.ibatis.annotations.Param;

public interface TrainQualityMapper extends BaseMapper<TrainQuality> {
    // 质量评价列表分页查询（支持10/20/50/100条/页）
    IPage<TrainQuality> selectQualityList(Page<TrainQuality> page,
                                          @Param("userId") Long userId,
                                          @Param("orgId") String orgId);

    // 根据评价ID查询答题详情（得分、正确/错误题目数量）
    TrainQuality selectQualityDetail(@Param("qualityId") Long qualityId);
}