package com.train.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.train.entity.SysUser;
import com.train.entity.SysUserRole;
import com.train.exception.BusinessException;
import com.train.mapper.SysUserMapper;
import com.train.mapper.SysUserRoleMapper;
import com.train.service.ISysOrgRoleService;
import com.train.service.ISysUserService;
import com.train.service.SysUserRoleService;
import com.train.util.AESUtil;
import com.train.util.Md5Util;
import com.train.util.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务实现类
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Resource
    private SysUserRoleService sysUserRoleService;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private ISysOrgRoleService sysOrgRoleService;

    @Override
    public IPage<SysUser> queryUserPage(Page<SysUser> page, String account, String userName, String orgId, String currentOrgId, Integer isSysAdmin) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(account), SysUser::getAccount, account)
                .like(StringUtils.isNotBlank(userName), SysUser::getUserName, userName)
                .orderByDesc(SysUser::getCreateTime);
        // 权限隔离：超级管理员查所有，机构管理员查本机构
        if (isSysAdmin != 1) {
            queryWrapper.eq(StringUtils.isNotBlank(currentOrgId), SysUser::getOrgId, currentOrgId);
        } else if (StringUtils.isNotBlank(orgId)) {
            // 超级管理员可按机构筛选
            queryWrapper.eq(SysUser::getOrgId, orgId);
        }
        return this.baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addUser(SysUser sysUser, List<String> roleIds) {
        // 参数校验
        if (sysUser == null || StringUtils.isBlank(sysUser.getAccount()) || StringUtils.isBlank(sysUser.getUserName())) {
            throw new BusinessException("用户账号和用户名不能为空");
        }
        if (StringUtils.isBlank(sysUser.getOrgId())) {
            throw new BusinessException("机构ID不能为空");
        }

        // 1. 校验账号唯一性
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getAccount, sysUser.getAccount()).eq(SysUser::getOrgId, sysUser.getOrgId());
        if (this.baseMapper.exists(queryWrapper)) {
            throw new BusinessException("用户账号已存在，请更换");
        }
        // 2. 补全默认值
        if (sysUser.getType() == null) {
            sysUser.setType(1); // 默认普通用户
        }
        if (sysUser.getStatus() == null) {
            sysUser.setStatus(1); // 默认正常状态
        }
        if (sysUser.getIsSysAdmin() == null) {
            sysUser.setIsSysAdmin(0); // 默认非超级管理员
        }
        if (sysUser.getCanDelete() == null) {
            sysUser.setCanDelete(1); // 默认可删除
        }
        sysUser.setCreateTime(LocalDateTime.now());
        // 3. 密码加密（默认a123456，若前端未传）
        if (StringUtils.isBlank(sysUser.getPassword())) {
            sysUser.setPassword(Md5Util.encrypt("a123456"));
        } else {
            // 对接收到的AES加密密码进行解密
            sysUser.setPassword(AESUtil.desEncrypt(sysUser.getPassword()));
            sysUser.setPassword(Md5Util.encrypt(sysUser.getPassword()));
        }

        // 4. 保存用户
        boolean saveFlag = this.save(sysUser);
        if (!saveFlag) {
            throw new BusinessException("用户新增失败");
        }

        // 5. 分配角色
        this.saveUserRole(sysUser.getUserId(), roleIds);

        return Result.success("用户新增成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> editUser(SysUser sysUser, List<String> roleIds) {
        if (sysUser == null || sysUser.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        // 1. 校验用户是否可修改（超级管理员不可修改）
        SysUser oldUser = this.getById(sysUser.getUserId());
        if (oldUser == null) {
            throw new BusinessException("用户不存在");
        }
        if (oldUser.getIsSysAdmin() == 1) {
            throw new BusinessException("系统超级管理员不可修改");
        }
        // 2. 校验账号唯一性（排除自身）
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getAccount, sysUser.getAccount())
                .ne(SysUser::getUserId, sysUser.getUserId());
        if (this.baseMapper.exists(queryWrapper)) {
            throw new BusinessException("用户账号已存在，请更换");
        }
        // 3.
        sysUser.setPassword(AESUtil.desEncrypt(sysUser.getPassword()));
        sysUser.setPassword(Md5Util.encrypt(sysUser.getPassword()));
        // 4. 更新用户
        boolean updateFlag = this.updateById(sysUser);
        if (!updateFlag) {
            throw new BusinessException("用户编辑失败");
        }
        // 5. 更新角色
        this.saveUserRole(sysUser.getUserId(), roleIds);
        return Result.success("用户编辑成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        // 1. 校验用户是否可删除
        SysUser sysUser = this.getById(userId);
        if (sysUser == null) {
            throw new BusinessException("用户不存在");
        }
        if (sysUser.getIsSysAdmin() == 1 || sysUser.getCanDelete() == 0) {
            throw new BusinessException("该用户不可删除");
        }
        // 2. 校验是否为机构最后一名管理员
        this.checkOrgAdminLast(userId, sysUser.getOrgId());
        // 3. 删除用户及关联角色
        this.baseMapper.deleteById(userId);
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(userRoleWrapper);
        return Result.success("用户删除成功");
    }

    @Override
    public Result<?> changeUserStatus(Long userId, Integer status) {
        if (userId == null || status == null) {
            throw new BusinessException("用户ID和状态不能为空");
        }
        SysUser sysUser = this.getById(userId);
        if (sysUser == null) {
            throw new BusinessException("用户不存在");
        }
        // 超级管理员不可修改状态
        if (sysUser.getIsSysAdmin() == 1) {
            throw new BusinessException("系统超级管理员不可修改状态");
        }
        sysUser.setStatus(status);
        boolean updateFlag = this.updateById(sysUser);
        if (!updateFlag) {
            throw new BusinessException("用户状态切换失败");
        }
        return Result.success(status == 1 ? "用户启用成功" : "用户禁用成功");
    }

    @Override
    public Result<?> resetUserPwd(Long userId, String newPassword) {
        if (userId == null || StringUtils.isBlank(newPassword)) {
            throw new BusinessException("用户ID和新密码不能为空");
        }
        SysUser sysUser = this.getById(userId);
        if (sysUser == null) {
            throw new BusinessException("用户不存在");
        }
        // 超级管理员不可重置密码（可自行修改）
        if (sysUser.getIsSysAdmin() == 1) {
            throw new BusinessException("系统超级管理员不可重置密码");
        }
        // 密码加密
        sysUser.setPassword(Md5Util.encrypt(newPassword));
        boolean updateFlag = this.updateById(sysUser);
        if (!updateFlag) {
            throw new BusinessException("密码重置失败");
        }
        return Result.success("密码重置成功");
    }

    /**
     * 保存用户-角色关联关系（先删后加）
     */
    private void saveUserRole(Long userId, List<String> roleIds) {
        // 1. 删除原有关联
        LambdaQueryWrapper<SysUserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(queryWrapper);
        // 2. 新增新关联
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysUserRole> userRoleList = roleIds.stream()
                .map(roleId -> {
                    SysUserRole sysUserRole = new SysUserRole();
                    sysUserRole.setUserId(userId);
                    sysUserRole.setRoleId(roleId);
                    return sysUserRole;
                }).collect(Collectors.toList());
        sysUserRoleService.saveBatch(userRoleList);
    }

    /**
     * 校验是否为机构最后一名管理员
     */
    private void checkOrgAdminLast(Long userId, String orgId) {
        if (StringUtils.isBlank(orgId)) {
            return;
        }
        // 1. 查询该用户是否为机构管理员
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, userId)
                .apply("role_id IN (SELECT role_id FROM sys_role WHERE role_name LIKE '%机构管理员角色%' AND org_id = {0})", orgId);
        boolean isOrgAdmin = sysUserRoleMapper.exists(userRoleWrapper);
        if (!isOrgAdmin) {
            return;
        }
        // 2. 查询该机构下机构管理员总数
        LambdaQueryWrapper<SysUserRole> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.apply("role_id IN (SELECT role_id FROM sys_role WHERE role_name LIKE '%机构管理员角色%' AND org_id = {0})", orgId);
        Long orgAdminCount = sysUserRoleMapper.selectCount(countWrapper);
        // 3. 仅剩余1名管理员，不可删除
        if (orgAdminCount <= 1) {
            throw new BusinessException("该机构仅剩余1名管理员，不可删除，请先新增其他管理员");
        }
    }
}
