package com.train.enums;

// 操作类型枚举
public enum OperateType {
    ADD("新增"),
    EDIT("编辑"),
    DELETE("删除"),
    QUERY("查询");

    private final String desc;
    OperateType(String desc) {
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }
}
