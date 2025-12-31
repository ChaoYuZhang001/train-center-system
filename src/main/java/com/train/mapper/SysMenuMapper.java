package com.train.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.train.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    /**
     * 根据菜单ID列表查询权限标识
     * @param menuIds 菜单ID列表
     * @return 权限标识列表（如：sys:org:list）
     */
    List<String> selectPermsByMenuIds(@Param("menuIds") List<Long> menuIds);

    /**
     * 根据角色ID列表查询关联的权限标识
     * @param roleIds 角色ID列表
     * @return 权限标识列表
     */
    List<String> selectPermsByRoleIds(@Param("roleIds") List<String> roleIds);

    /**
     * 根据角色ID列表查询关联的菜单权限
     * @param roleIds 角色ID列表
     * @return 菜单列表
     */
    List<SysMenu> selectMenuListByRoleIds(@Param("roleIds") List<String> roleIds);

    /**
     * 查询所有启用的菜单（树形结构基础数据）
     * @return 菜单列表
     */
    List<SysMenu> selectAllEnableMenu();
}