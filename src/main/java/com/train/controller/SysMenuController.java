package com.train.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.annotation.RequiresPermission;
import com.train.entity.SysMenu;
import com.train.entity.SysUser;
import com.train.entity.SysUserRole;
import com.train.mapper.SysUserMapper;
import com.train.mapper.SysUserRoleMapper;
import com.train.security.JwtUserDetails;
import com.train.service.ISysMenuService;
import com.train.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理控制器
 */
@Tag(name = "系统管理-菜单管理", description = "菜单分页查询、树形查询、新增、编辑、删除、状态切换接口")
@RestController
@RequestMapping("/train/sys/menu")
public class SysMenuController {

    @Resource
    private ISysMenuService sysMenuService;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;


    /**
     * 菜单树形查询（所有启用菜单）
     */
    @Operation(summary = "菜单树形查询", description = "返回树形结构菜单，用于前端树形展示和权限分配")
    @GetMapping("/tree")
    public Result<List<SysMenu>> queryMenuTree() {
        List<SysMenu> menuTree = sysMenuService.queryMenuTree();
        return Result.success(menuTree);
    }

    /**
     * 查询当前用户权限菜单（树形结构）
     */
    @Operation(summary = "当前用户权限菜单", description = "返回当前登录用户拥有的权限菜单，用于前端动态渲染菜单")
    @GetMapping("/currentUserPermTree")
    public Result<List<SysMenu>> queryCurrentUserPermMenuTree() {
        // 1. 获取当前登录用户
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SysUser currentUser = sysUserMapper.selectUserByAccount(userDetails.getAccount(),userDetails.getOrgId());
        // 2. 查询用户关联的角色ID
        List<String> roleIds = sysUserRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>()
                                .eq(SysUserRole::getUserId, currentUser.getUserId())
                ).stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        // 3. 查询角色关联的权限菜单
        List<SysMenu> permMenuTree = sysMenuService.queryPermMenuTreeByRoleIds(roleIds);
        return Result.success(permMenuTree);
    }
}