package com.train.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.train.entity.SysOrg;
import com.train.util.Result;

import java.util.List;

/**
 * 机构业务接口
 */
public interface ISysOrgService extends IService<SysOrg> {
    /**
     * 机构分页查询（支持模糊查询）
     *
     * @param page         分页参数
     * @param name
     * @param orgName      机构名称（模糊）
     * @param adminAccount 管理员账号（模糊）
     * @param currentOrgId 当前登录用户所属机构ID（机构管理员仅查本机构）
     * @param isSysAdmin   是否系统超级管理员
     * @return 分页结果
     */
    IPage<SysOrg> queryOrgPage(Page<SysOrg> page,String orgName, String adminAccount, String adminName, String currentOrgId, Integer isSysAdmin);

    /**
     * 新增机构（含默认管理员创建）
     * @param sysOrg 机构信息
     * @return 操作结果
     */
    Result<?> addOrg(SysOrg sysOrg);

    /**
     * 编辑机构（校验名称唯一性）
     * @param sysOrg 机构信息
     * @return 操作结果
     */
    Result<?> editOrg(SysOrg sysOrg);

    /**
     * 删除机构（超级管理员专属，机构管理员不可删除）
     * @param orgId 机构ID
     * @return 操作结果
     */
    Result<?> deleteOrg(String orgId);

    /**
     * 切换机构状态（启用/禁用）
     * @param orgId 机构ID
     * @param status 目标状态（0=禁用，1=启用）
     * @return 操作结果
     */
    Result<?> changeOrgStatus(String orgId, Integer status);

    /**
     * 查询所有启用的机构
     * @return 机构列表
     */
    List<SysOrg> queryAllEnableOrg();
}