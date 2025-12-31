package com.train.enums;

// 操作菜单枚举
public enum OperateMenu {
    LOGIN_LOGOUT("登录登出"),
    TRAIN_MANAGEMENT("培训中心"),
    ORG_MANAGEMENT("机构管理"),
    ROLE_MANAGEMENT("角色管理"),
    USER_MANAGEMENT("用户管理");

    private final String desc;
    OperateMenu(String desc) {
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }
}