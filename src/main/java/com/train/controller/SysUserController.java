package com.train.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.annotation.RequiresPermission;
import com.train.entity.SysUser;
import com.train.mapper.SysUserMapper;
import com.train.security.JwtUserDetails;
import com.train.service.ISysUserService;
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
 * 用户管理控制器
 */
@Tag(name = "系统管理-用户管理", description = "用户分页查询、新增、编辑、删除、密码重置接口")
@RestController
@RequestMapping("/train/sys/user")
public class SysUserController {

    @Resource
    private ISysUserService sysUserService;

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 用户分页查询
     */
    @Operation(summary = "用户分页查询", description = "支持账号、用户名模糊查询，按权限隔离数据")
    @RequiresPermission("user:read")
    @GetMapping("/page")
    public Result<IPage<SysUser>> queryUserPage(
            @Parameter(name = "pageNum", description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(name = "pageSize", description = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(name = "account", description = "登录账号（模糊查询）")
            @RequestParam(required = false) String account,
            @Parameter(name = "userName", description = "用户名（模糊查询）")
            @RequestParam(required = false) String userName,
            @Parameter(name = "orgId", description = "所属机构ID（超级管理员专用）")
            @RequestParam(required = false) String orgId) {
        // 获取当前登录用户
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SysUser currentUser = sysUserMapper.selectUserByAccount(userDetails.getUsername(),userDetails.getOrgId());
        // 分页查询
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        IPage<SysUser> userPage = sysUserService.queryUserPage(
                page, account, userName, orgId, currentUser.getOrgId(), currentUser.getIsSysAdmin()
        );
        return Result.success(userPage);
    }

    /**
     * 新增用户
     */
    @Operation(summary = "新增用户", description = "同时分配角色，密码默认a123456")
    @RequiresPermission("user:add")
    @PostMapping("/add")
    public Result<?> addUser(
            @RequestBody SysUser sysUser,
            @Parameter(name = "roleIds", description = "角色ID列表", required = true)
            @RequestParam List<String> roleIds) {
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        sysUser.setOrgId(userDetails.getOrgId());
        sysUser.setCreateBy(userDetails.getAccount());
        return sysUserService.addUser(sysUser, roleIds);
    }

    /**
     * 编辑用户
     */
    @Operation(summary = "编辑用户", description = "同时更新角色，不修改密码，系统超级管理员不可编辑")
    @RequiresPermission("user:edit")
    @PutMapping("/edit")
    public Result<?> editUser(
            @RequestBody SysUser sysUser,
            @RequestParam List<String> roleIds) {
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        sysUser.setOrgId(userDetails.getOrgId());
        return sysUserService.editUser(sysUser, roleIds);
    }

    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "机构最后一名管理员不可删除，超级管理员不可删除")
    @RequiresPermission("user:remove")
    @DeleteMapping("/delete/{userId}")
    public Result<?> deleteUser(@PathVariable Long userId) {
        return sysUserService.deleteUser(userId);
    }

    /**
     * 切换用户状态
     */
    @Operation(summary = "切换用户状态", description = "启用/禁用用户，禁用后用户无法登录")
    @RequiresPermission("user:edit")
    @PutMapping("/changeStatus")
    public Result<?> changeUserStatus(
            @RequestParam Long userId,
            @RequestParam Integer status) {
        return sysUserService.changeUserStatus(userId, status);
    }

    /**
     * 重置用户密码
     */
    @Operation(summary = "重置用户密码", description = "密码自动加密，超级管理员不可重置")
    @RequiresPermission("user:edit")
    @PutMapping("/resetPwd")
    public Result<?> resetUserPwd(
            @RequestParam Long userId,
            @RequestParam String newPassword) {
        return sysUserService.resetUserPwd(userId, newPassword);
    }
}