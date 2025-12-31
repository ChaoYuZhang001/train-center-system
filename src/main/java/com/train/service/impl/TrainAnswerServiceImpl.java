package com.train.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.train.entity.TrainAnswer;
import com.train.mapper.TrainAnswerMapper;
import com.train.service.TrainAnswerService;
import org.springframework.stereotype.Service;

@Service
public class TrainAnswerServiceImpl extends ServiceImpl<TrainAnswerMapper, TrainAnswer>  implements TrainAnswerService {
}
