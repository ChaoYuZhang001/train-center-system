package com.train.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.train.entity.SysRole;
import com.train.entity.SysUser;
import com.train.util.Result;

import java.util.List;

/**
 * 角色业务接口
 */
public interface ISysRoleService extends IService<SysRole> {
    /**
     * 角色分页查询
     * @param page 分页参数
     * @param roleName 角色名称（模糊）
     * @param orgId 所属机构ID
     * @param currentOrgId 当前登录用户所属机构ID
     * @param isSysAdmin 是否系统超级管理员
     * @return 分页结果
     */
    IPage<SysRole> queryRolePage(Page<SysRole> page, String roleName, String orgId, String currentOrgId, Integer isSysAdmin);

    /**
     * 新增角色（含菜单权限分配）
     *
     * @param currentOrgId
     * @param sysRole 角色信息
     * @param menuIds 菜单ID列表
     * @return 操作结果
     */
    Result<?> addRole(SysUser currentUser, SysRole sysRole, List<Long> menuIds);

    /**
     * 编辑角色（含菜单权限更新）
     *
     * @param currentOrgId
     * @param sysRole 角色信息
     * @param menuIds 菜单ID列表
     * @return 操作结果
     */
    Result<?> editRole(String currentOrgId, SysRole sysRole, List<Long> menuIds);

    /**
     * 删除角色（校验是否可删除）
     * @param roleId 角色ID
     * @return 操作结果
     */
    Result<?> deleteRole(String roleId);

    /**
     * 切换角色状态
     * @param roleId 角色ID
     * @param status 目标状态
     * @return 操作结果
     */
    Result<?> changeRoleStatus(String roleId, Integer status);

    /**
     * 分配角色权限（单独更新菜单权限）
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 操作结果
     */
    Result<?> assignRolePerm(String roleId, List<Long> menuIds);

    /**
     * 查询某机构下所有启用的角色
     * @param orgId 机构ID
     * @return 角色列表
     */
    List<SysRole> queryRoleListByOrgId(String orgId);
}