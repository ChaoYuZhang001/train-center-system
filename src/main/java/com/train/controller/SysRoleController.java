package com.train.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.annotation.RequiresPermission;
import com.train.entity.SysRole;
import com.train.entity.SysUser;
import com.train.mapper.SysUserMapper;
import com.train.security.JwtUserDetails;
import com.train.service.ISysRoleService;
import com.train.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "系统管理-角色管理", description = "角色分页查询、新增、编辑、删除、权限分配接口")
@RestController
@RequestMapping("/train/sys/role")
public class SysRoleController {

    @Resource
    private ISysRoleService sysRoleService;

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 角色分页查询
     */
    @Operation(summary = "角色分页查询", description = "支持角色名称模糊查询，按权限隔离数据")
    @RequiresPermission("role:read")
    @GetMapping("/page")
    public Result<IPage<SysRole>> queryRolePage(
            @Parameter(name = "pageNum", description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(name = "pageSize", description = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(name = "roleName", description = "角色名称（模糊查询）")
            @RequestParam(required = false) String roleName,
            @Parameter(name = "orgId", description = "所属机构ID（超级管理员专用）")
            @RequestParam(required = false) String orgId) {
        // 获取当前登录用户
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SysUser currentUser = sysUserMapper.selectUserByAccount(userDetails.getAccount(),userDetails.getOrgId());
        // 分页查询
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        IPage<SysRole> rolePage = sysRoleService.queryRolePage(
                page, roleName, orgId, currentUser.getOrgId(), currentUser.getIsSysAdmin()
        );
        return Result.success(rolePage);
    }

    /**
     * 新增角色
     */
    @Operation(summary = "新增角色", description = "同时分配菜单权限，角色名称同一机构内唯一")
    @RequiresPermission("role:add")
    @PostMapping("/add")
    public Result<?> addRole(
            @RequestBody SysRole sysRole,
            @Parameter(name = "menuIds", description = "菜单ID列表", required = true)
            @RequestParam List<Long> menuIds) {
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SysUser currentUser = sysUserMapper.selectUserByAccount(userDetails.getAccount(),userDetails.getOrgId());
        return sysRoleService.addRole(currentUser, sysRole, menuIds);
    }

    /**
     * 编辑角色
     */
    @Operation(summary = "编辑角色", description = "同时更新菜单权限，系统超级管理员角色不可编辑")
    @RequiresPermission("role:edit")
    @PutMapping("/edit")
    public Result<?> editRole(
            @RequestBody SysRole sysRole,
            @RequestParam List<Long> menuIds) {
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SysUser currentUser = sysUserMapper.selectUserByAccount(userDetails.getAccount(),userDetails.getOrgId());

        return sysRoleService.editRole(currentUser.getOrgId(), sysRole, menuIds);
    }

    /**
     * 删除角色
     */
    @Operation(summary = "删除角色", description = "已关联用户的角色不可删除，不可删除角色禁止操作")
    @RequiresPermission("role:remove")
    @DeleteMapping("/delete/{roleId}")
    public Result<?> deleteRole(@PathVariable String roleId) {
        return sysRoleService.deleteRole(roleId);
    }

    /**
     * 切换角色状态
     */
    @Operation(summary = "切换角色状态", description = "启用/禁用角色，禁用后关联用户失去该角色权限")
    @RequiresPermission("role:edit")
    @PutMapping("/changeStatus")
    public Result<?> changeRoleStatus(
            @RequestParam String roleId,
            @RequestParam Integer status) {
        return sysRoleService.changeRoleStatus(roleId, status);
    }

    /**
     * 查询某机构下所有启用角色
     */
    @Operation(summary = "查询机构启用角色", description = "用于用户分配角色时的选择")
    @RequiresPermission("role:read")
    @GetMapping("/listByOrg/{orgId}")
    public Result<List<SysRole>> queryRoleListByOrgId(@PathVariable String orgId) {
        List<SysRole> roleList = sysRoleService.queryRoleListByOrgId(orgId);
        return Result.success(roleList);
    }
}