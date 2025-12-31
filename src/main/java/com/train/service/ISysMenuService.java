package com.train.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.train.entity.SysMenu;
import com.train.util.Result;

import java.util.List;

/**
 * 菜单业务接口
 */
public interface ISysMenuService extends IService<SysMenu> {
    /**
     * 菜单分页查询（支持模糊查询）
     * @param page 分页参数
     * @param menuName 菜单名称（模糊）
     * @param type 菜单类型（0/1/2）
     * @return 分页结果
     */
    IPage<SysMenu> queryMenuPage(Page<SysMenu> page, String menuName, Integer type);

    /**
     * 查询菜单树形结构（所有启用菜单，用于前端树形展示）
     * @return 树形菜单列表
     */
    List<SysMenu> queryMenuTree();

    /**
     * 新增菜单（校验父级合法性、菜单名称唯一性）
     * @param sysMenu 菜单信息
     * @return 操作结果
     */
    Result<?> addMenu(SysMenu sysMenu);

    /**
     * 编辑菜单（校验父级合法性、排除自身作为父级、名称唯一性）
     * @param sysMenu 菜单信息
     * @return 操作结果
     */
    Result<?> editMenu(SysMenu sysMenu);

    /**
     * 删除菜单（校验是否有子菜单、是否被角色关联）
     * @param menuId 菜单ID
     * @return 操作结果
     */
    Result<?> deleteMenu(Long menuId);

    /**
     * 切换菜单状态（启用/禁用）
     * @param menuId 菜单ID
     * @param status 目标状态（0=禁用，1=启用）
     * @return 操作结果
     */
    Result<?> changeMenuStatus(Long menuId, Integer status);

    /**
     * 根据角色ID列表查询用户权限菜单（树形结构）
     * @param roleIds 角色ID列表
     * @return 权限菜单树形结构
     */
    List<SysMenu> queryPermMenuTreeByRoleIds(List<String> roleIds);
}