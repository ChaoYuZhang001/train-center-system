package com.train.enums;


// 操作结果枚举
public enum OperateResult {
    SUCCESS("成功"),
    FAIL("失败");

    private final String desc;
    OperateResult(String desc) {
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }
}