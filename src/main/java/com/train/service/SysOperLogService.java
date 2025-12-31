package com.train.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.train.dto.SysOperLogQuery;
import com.train.entity.SysOperLog;

import java.util.List;

public interface SysOperLogService  extends IService<SysOperLog> {
    List<SysOperLog> selectForExport(SysOperLogQuery query);
}
