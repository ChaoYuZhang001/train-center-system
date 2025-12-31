//package com.train.controller; TODO 没有日志需求
//
//
//import com.train.entity.SysOperLog;
//import com.train.service.SysOperLogService;
//import com.train.dto.SysOperLogQuery;
//import com.train.util.ExcelUtil;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import javax.servlet.http.HttpServletResponse;
//import java.util.List;
//
//@Tag(name = "操作日志管理", description = "操作日志管理接口")
//@RestController
//@RequestMapping("/sysOperLog")
//public class SysOperLogController {
//
//    @Autowired
//    private SysOperLogService sysOperLogService;
//
//    @Operation(summary = "导出操作日志", description = "导出操作日志")
//    @PostMapping("/export")
//    public void export(@RequestBody SysOperLogQuery query, HttpServletResponse response) {
//        List<SysOperLog> list = sysOperLogService.selectForExport(query);
//        ExcelUtil excel = new ExcelUtil<SysOperLog>(SysOperLog.class);
//        excel.exportExcel(response,list, "操作日志");
//    }
//}