package com.train.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.train.entity.SysUser;
import com.train.util.Result;

import java.util.List;

/**
 * 用户业务接口
 */
public interface ISysUserService extends IService<SysUser> {
    /**
     * 用户分页查询
     * @param page 分页参数
     * @param account 账号（模糊）
     * @param userName 用户名（模糊）
     * @param orgId 所属机构ID
     * @param currentOrgId 当前登录用户所属机构ID
     * @param isSysAdmin 是否系统超级管理员
     * @return 分页结果
     */
    IPage<SysUser> queryUserPage(Page<SysUser> page, String account, String userName, String orgId, String currentOrgId, Integer isSysAdmin);

    /**
     * 新增用户（含角色分配）
     *
     * @param sysUser 用户信息
     * @param roleIds 角色ID列表
     * @return 操作结果
     */
    Result<?> addUser(SysUser sysUser, List<String> roleIds);

    /**
     * 编辑用户（含角色更新）
     * @param sysUser 用户信息
     * @param roleIds 角色ID列表
     * @return 操作结果
     */
    Result<?> editUser(SysUser sysUser, List<String> roleIds);

    /**
     * 删除用户（校验是否可删除）
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<?> deleteUser(Long userId);

    /**
     * 切换用户状态
     * @param userId 用户ID
     * @param status 目标状态
     * @return 操作结果
     */
    Result<?> changeUserStatus(Long userId, Integer status);

    /**
     * 重置用户密码
     * @param userId 用户ID
     * @param newPassword 新密码（明文，自动加密）
     * @return 操作结果
     */
    Result<?> resetUserPwd(Long userId, String newPassword);
}