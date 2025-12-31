package com.train.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.train.annotation.RequiresPermission;
import com.train.entity.SysOrg;
import com.train.entity.SysUser;
import com.train.mapper.SysUserMapper;
import com.train.security.JwtUserDetails;
import com.train.service.ISysOrgService;
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
 * 机构管理控制器
 */
@Tag(name = "系统管理-机构管理", description = "机构分页查询、新增、编辑、删除、状态切换接口")
@RestController
@RequestMapping("/train/sys/org")
public class SysOrgController {

    @Resource
    private ISysOrgService sysOrgService;

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 机构分页查询
     */
    @Operation(summary = "机构分页查询", description = "支持机构名称、管理员账号模糊查询，按权限隔离数据")
    @RequiresPermission("org:read")
    @GetMapping("/page")
    public Result<IPage<SysOrg>> queryOrgPage(
            @Parameter(name = "pageNum", description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(name = "pageSize", description = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(name = "orgName", description = "机构名称（模糊查询）")
            @RequestParam(required = false) String orgName,
            @Parameter(name = "adminAccount", description = "管理员账号（模糊查询）")
            @RequestParam(required = false) String adminAccount,
            @Parameter(name = "adminName", description = "管理员姓名（模糊查询）")
            @RequestParam(required = false) String adminName
            ) {
        // 获取当前登录用户信息
        JwtUserDetails userDetails = (JwtUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SysUser currentUser = sysUserMapper.selectUserByAccount(userDetails.getAccount(),userDetails.getOrgId());
        // 分页查询
        Page<SysOrg> page = new Page<>(pageNum, pageSize);
        IPage<SysOrg> orgPage = sysOrgService.queryOrgPage(
                page, orgName, adminAccount,adminName,
                currentUser.getOrgId(), currentUser.getIsSysAdmin()
        );
        return Result.success(orgPage);
    }

    /**
     * 新增机构
     */
    @Operation(summary = "新增机构", description = "自动创建默认机构管理员，密码默认a123456")
    @RequiresPermission("org:add")
    @PostMapping("/add")
    public Result<?> addOrg(@RequestBody SysOrg sysOrg) {
        return sysOrgService.addOrg(sysOrg);
    }

    /**
     * 编辑机构
     */
    @Operation(summary = "编辑机构", description = "校验机构名称唯一性，不修改管理员密码")
    @RequiresPermission("org:edit")
    @PutMapping("/edit")
    public Result<?> editOrg(@RequestBody SysOrg sysOrg) {
        return sysOrgService.editOrg(sysOrg);
    }

    /**
     * 删除机构
     */
    @Operation(summary = "删除机构", description = "仅系统超级管理员可操作，级联删除关联用户/角色")
    @RequiresPermission("org:remove")
    @DeleteMapping("/delete/{orgId}")
    public Result<?> deleteOrg(@PathVariable String orgId) {
        return sysOrgService.deleteOrg(orgId);
    }

    /**
     * 切换机构状态
     */
    @Operation(summary = "切换机构状态", description = "启用/禁用机构，禁用后机构内用户无法登录")
    @RequiresPermission("org:edit")
    @PutMapping("/changeStatus")
    public Result<?> changeOrgStatus(
            @Parameter(name = "orgId", description = "机构ID", required = true)
            @RequestParam String orgId,
            @Parameter(name = "status", description = "目标状态（0=禁用，1=启用）", required = true)
            @RequestParam Integer status) {
        return sysOrgService.changeOrgStatus(orgId, status);
    }

    /**
     * 查询所有启用的机构
     */
    @Operation(summary = "查询所有启用机构", description = "用于登录选择登录哪个机构")
//    @RequiresPermission("org:read")
    @GetMapping("/allEnable")
    public Result<List<SysOrg>> queryAllEnableOrg() {
        List<SysOrg> orgList = sysOrgService.queryAllEnableOrg();
        return Result.success(orgList);
    }

}