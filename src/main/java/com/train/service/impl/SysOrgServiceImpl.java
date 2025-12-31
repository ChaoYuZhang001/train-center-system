package com.train.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.train.constant.Constants;
import com.train.entity.*;
import com.train.exception.BusinessException;
import com.train.mapper.SysOrgMapper;
import com.train.service.*;
import com.train.util.AESUtil;
import com.train.util.Md5Util;
import com.train.util.Result;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.train.constant.Constants.ROLES.*;

/**
 * 机构业务实现类
 */
@Service
public class SysOrgServiceImpl extends ServiceImpl<SysOrgMapper, SysOrg> implements ISysOrgService {

    @Resource
    private ISysUserService sysUserService;
    @Resource
    private ISysRoleService sysRoleService;
    @Resource
    private SysUserRoleService sysUserRoleService;
    @Resource
    private ISysOrgRoleService sysOrgRoleService;

    private static final String DEFAULT_ADMIN_PASSWORD = "a123456";
    private static final DateTimeFormatter ORG_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger dailySequence = new AtomicInteger(1);
    private static final AtomicReference<LocalDate> currentDate = new AtomicReference<>(LocalDate.now());

    @Override
    public IPage<SysOrg> queryOrgPage(Page<SysOrg> page, String orgName, String adminAccount,String adminName, String currentOrgId, Integer isSysAdmin) {
        // 参数验证
        if (page == null) {
            throw new BusinessException("分页参数不能为空");
        }

        LambdaQueryWrapper<SysOrg> queryWrapper = buildBaseQueryWrapper(orgName, adminAccount,adminName);

        // 权限隔离：机构管理员仅查询本机构
        if (isSysAdmin != 1 && StringUtils.isNotBlank(currentOrgId)) {
            queryWrapper.eq(SysOrg::getOrgId, currentOrgId);
        }
        queryWrapper.eq(SysOrg::getStatus, 1)
        .ne(SysOrg::getOrgId, Constants.SYS_ORG_ID);
        return this.baseMapper.selectPage(page, queryWrapper);
    }

    /**
     * 构建基础查询条件
     */
    private LambdaQueryWrapper<SysOrg> buildBaseQueryWrapper(String orgName, String adminAccount, String adminName) {
        LambdaQueryWrapper<SysOrg> queryWrapper = new LambdaQueryWrapper<>();
        // 模糊查询，允许参数为null
        queryWrapper.like(StringUtils.isNotBlank(orgName), SysOrg::getOrgName, orgName)
                .like(StringUtils.isNotBlank(adminAccount), SysOrg::getAdminAccount, adminAccount)
                .like(StringUtils.isNotBlank(adminName), SysOrg::getAdminName, adminName)
                .orderByDesc(SysOrg::getCreateTime);
        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addOrg(SysOrg sysOrg) {
        validateAddOrgParams(sysOrg);
        SysUser defaultAdmin = new SysUser();
        defaultAdmin.setAccount(sysOrg.getAdminAccount());
        defaultAdmin.setUserName(sysOrg.getAdminName());
        defaultAdmin.setPassword(sysOrg.getAdminPassword());
        defaultAdmin.setType(1);
        defaultAdmin.setStatus(1);
        defaultAdmin.setOrgId(sysOrg.getOrgId());
        defaultAdmin.setIsSysAdmin(0);
        defaultAdmin.setCanDelete(1);
        defaultAdmin.setCreateBy("系统自动创建");
        defaultAdmin.setCreateTime(LocalDateTime.now());
        if (!StringUtils.isBlank(sysOrg.getAdminPassword())) {
            // 对接收到的AES加密密码进行解密
            sysOrg.setAdminPassword(AESUtil.desEncrypt(sysOrg.getAdminPassword()));
        }
        validatePassword(sysOrg.getAdminPassword());

        // 1. 校验机构名称唯一性
        checkOrgNameUnique(sysOrg.getOrgName(), null);

        // 2. 补全默认值
        completeOrgDefaults(sysOrg);

        // 3. 生成机构ID
        generateOrgId(sysOrg);

        // 4. 保存机构
        boolean saveFlag = this.save(sysOrg);
        if (!saveFlag) {
            throw new BusinessException("机构新增失败");
        }
        // 5. 创建默认管理员
        createDefaultAdmin(defaultAdmin,sysOrg);

        List<SysOrgRole> sysOrgRoles = Lists.newArrayList();
        SysOrgRole sysOrgRole = new SysOrgRole();
        sysOrgRole.setOrgId(sysOrg.getOrgId());
        sysOrgRole.setRoleId(ORG_ROLE_ADMIN);
        sysOrgRoles.add(sysOrgRole);
        SysOrgRole sysOrgRole1 = new SysOrgRole();
        sysOrgRole1.setOrgId(sysOrg.getOrgId());
        sysOrgRole1.setRoleId(ORG_ROLE_USER);
        sysOrgRoles.add(sysOrgRole1);
        sysOrgRoleService.saveBatch(sysOrgRoles);

        return Result.success("机构新增成功", sysOrg);
    }

    /**
     * 验证新增机构参数
     */
    private void validateAddOrgParams(SysOrg sysOrg) {
        if (sysOrg == null) {
            throw new BusinessException("机构信息不能为空");
        }
        if (StringUtils.isBlank(sysOrg.getOrgName())) {
            throw new BusinessException("机构名称不能为空");
        }
        if (StringUtils.length(sysOrg.getOrgName()) > 20) {
            throw new BusinessException("机构名称长度不能超过20个字符");
        }
        if (StringUtils.isBlank(sysOrg.getAdminAccount())) {
            throw new BusinessException("管理员账号不能为空");
        }
        if (StringUtils.length(sysOrg.getAdminAccount()) > 20) {
            throw new BusinessException("管理员账号长度不能超过20个字符");
        }
        if (StringUtils.isBlank(sysOrg.getAdminName())) {
            throw new BusinessException("管理员姓名不能为空");
        }
        if (StringUtils.length(sysOrg.getAdminName()) > 20) {
            throw new BusinessException("管理员姓名长度不能超过20个字符");
        }
    }

    /**
     * 验证密码格式和长度
     */
    private void validatePassword(String password) {
        if (StringUtils.isBlank(password)) {
            throw new BusinessException("管理员密码不能为空");
        }
        if (password.length() < 6 || password.length() > 12) {
            throw new BusinessException("管理员密码长度必须在6-12位之间");
        }
        // 验证密码格式：字母、数字
        if (!password.matches("^[a-zA-Z0-9]+$")) {
            throw new BusinessException("管理员密码只能包含字母和数字");
        }
    }

    /**
     * 检查机构名称唯一性
     */
    private void checkOrgNameUnique(String orgName, String excludeOrgId) {
        LambdaQueryWrapper<SysOrg> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysOrg::getOrgName, orgName);
        if (StringUtils.isNotBlank(excludeOrgId)) {
            queryWrapper.ne(SysOrg::getOrgId, excludeOrgId);
        }
        if (this.baseMapper.exists(queryWrapper)) {
            throw new BusinessException("机构名称已存在，请更换");
        }
    }

    /**
     * 补全机构默认值
     */
    private void completeOrgDefaults(SysOrg sysOrg) {
        if (sysOrg.getStatus() == null) {
            sysOrg.setStatus(1); // 默认启用
        }
        sysOrg.setCreateTime(LocalDateTime.now());

        // 机构管理员密码默认加密
        String password = StringUtils.isBlank(sysOrg.getAdminPassword()) ?
            DEFAULT_ADMIN_PASSWORD : sysOrg.getAdminPassword();
        sysOrg.setAdminPassword(Md5Util.encrypt(password));
    }

    /**
     * 生成机构ID
     */
    private void generateOrgId(SysOrg sysOrg) {
        try {
            LocalDate now = LocalDate.now();
            LocalDate current = currentDate.get();

            // 检查是否跨日期，需要重置序列号
            if (!now.equals(current)) {
                synchronized (SysOrgServiceImpl.class) {
                    // 双重检查，确保日期确实不同
                    LocalDate currentAfterSync = currentDate.get();
                    if (!now.equals(currentAfterSync)) {
                        if (currentDate.compareAndSet(currentAfterSync, now)) {
                            dailySequence.set(1);
                        }
                    }
                }
            }

            String datePart = now.format(ORG_ID_FORMATTER);
            int sequence = dailySequence.getAndIncrement();

            // 确保序列号在001-999范围内
            if (sequence > 999) {
                synchronized (SysOrgServiceImpl.class) {
                    if (dailySequence.get() > 999) {
                        dailySequence.set(1);
                    }
                    sequence = dailySequence.getAndIncrement();
                }
            }

            String sequenceStr = String.format("%03d", sequence);
            sysOrg.setOrgId(datePart + sequenceStr);
        } catch (Exception e) {
            throw new RuntimeException("生成机构ID失败", e);
        }
    }

    /**
     * 创建默认管理员
     */
    private void createDefaultAdmin(SysUser defaultAdmin, SysOrg sysOrg) {
        List<String> roleIds = Lists.newArrayList();
        roleIds.add(ORG_ROLE_ADMIN);
        defaultAdmin.setOrgId(sysOrg.getOrgId());
        sysUserService.addUser(defaultAdmin, roleIds);

        // 更新机构默认管理员ID
        sysOrg.setDefaultAdminUserId(defaultAdmin.getUserId());
        boolean updateFlag = this.updateById(sysOrg);
        if (!updateFlag) {
            throw new BusinessException("更新机构管理员ID失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> editOrg(SysOrg sysOrg) {
        if (sysOrg == null) {
            throw new BusinessException("机构信息不能为空");
        }
        if (StringUtils.isBlank(sysOrg.getOrgId())) {
            throw new BusinessException("机构ID不能为空");
        }
        if (StringUtils.isBlank(sysOrg.getOrgName())) {
            throw new BusinessException("机构名称不能为空");
        }
        if (StringUtils.length(sysOrg.getOrgName()) > 20) {
            throw new BusinessException("机构名称长度不能超过20个字符");
        }
        if (StringUtils.isBlank(sysOrg.getAdminAccount())) {
            throw new BusinessException("管理员账号不能为空");
        }
        if (StringUtils.length(sysOrg.getAdminAccount()) > 20) {
            throw new BusinessException("管理员账号长度不能超过20个字符");
        }
        if (StringUtils.isBlank(sysOrg.getAdminName())) {
            throw new BusinessException("管理员姓名不能为空");
        }
        if (StringUtils.length(sysOrg.getAdminName()) > 20) {
            throw new BusinessException("管理员姓名长度不能超过20个字符");
        }
        if (StringUtils.isBlank(sysOrg.getAdminPassword())) {
            throw new BusinessException("管理员密码不能为空");
        }
        // 对接收到的AES加密密码进行解密
        sysOrg.setAdminPassword(AESUtil.desEncrypt(sysOrg.getAdminPassword()));
        if (sysOrg.getAdminPassword().length() < 6 || sysOrg.getAdminPassword().length() > 12) {
            throw new BusinessException("管理员密码长度必须在6-12位之间");
        }
        // 验证密码格式：字母、数字
        if (!sysOrg.getAdminPassword().matches("^[a-zA-Z0-9]+$")) {
            throw new BusinessException("管理员密码只能包含字母和数字");
        }


        // 校验机构名称唯一性（排除自身）
        checkOrgNameUnique(sysOrg.getOrgName(), sysOrg.getOrgId());

        sysOrg.setAdminPassword(Md5Util.encrypt(sysOrg.getAdminPassword()));
        // 更新机构
        boolean updateFlag = this.updateById(sysOrg);
        if (!updateFlag) {
            throw new BusinessException("机构编辑失败");
        }
        //编辑用户信息
        SysUser sysUser = new SysUser();
        sysUser.setUserId(sysOrg.getDefaultAdminUserId());
        sysUser.setAccount(sysOrg.getAdminAccount());
        sysUser.setUserName(sysOrg.getAdminName());
        sysUser.setPassword(sysOrg.getAdminPassword());
        sysUserService.updateById(sysUser);
        handleOrgDisable(sysOrg.getOrgId());
        return Result.success("机构编辑成功");
    }

    /**
     * 处理机构禁用逻辑
     */
    private void handleOrgDisable(String orgId) {
        // 实现禁用机构下所有用户的逻辑
        // 例如：将该机构下所有用户状态设置为禁用
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteOrg(String orgId) {
        if (StringUtils.isBlank(orgId)) {
            throw new BusinessException("机构ID不能为空");
        }

        // 检查机构是否存在
        SysOrg existingOrg = this.getById(orgId);
        if (existingOrg == null) {
            throw new BusinessException("机构不存在");
        }


        // 删除相关机构信息
        deleteRelatedOrgInfo(orgId);

        // 删除机构（仅超级管理员可操作）
        boolean deleteFlag = this.removeById(orgId);
        if (!deleteFlag) {
            throw new BusinessException("机构删除失败");
        }

        return Result.success("机构删除成功");
    }


    private void deleteRelatedOrgInfo(String orgId) {
        // 实现删除相关机构信息的逻辑
        List<SysOrgRole> sysOrgRoleList = sysOrgRoleService.list(
            new QueryWrapper<SysOrgRole>().eq("org_id", orgId).select("role_id")
        );

        // 检查是否有相关角色
        if (!sysOrgRoleList.isEmpty()) {
            Set<String> roleIds = sysOrgRoleList.stream()
                .map(SysOrgRole::getRoleId)
                .collect(Collectors.toSet());

            // 只有在roleIds不为空时才执行删除
            if (!roleIds.isEmpty()) {
                sysOrgRoleService.remove(new QueryWrapper<SysOrgRole>().in("role_id", roleIds));
                // 验证roleIds中不包含保护角色ID
                List<String> safeRoleIds = roleIds.stream()
                        .filter(roleId -> !ORG_ROLE_ADMIN.equals(roleId)
                                && !ORG_ROLE_USER.equals(roleId)
                                && !SYS_ROLE_SUPER_ADMIN.equals(roleId))
                        .collect(Collectors.toList());
                sysRoleService.remove(new QueryWrapper<SysRole>().in("role_id", safeRoleIds));
            }
        }

        List<SysUser> userList = sysUserService.list(
            new QueryWrapper<SysUser>().eq("org_id", orgId).select("user_id")
        );

        // 检查是否有相关用户
        if (!userList.isEmpty()) {
            List<Long> userIds = userList.stream()
                .map(SysUser::getUserId)
                .collect(Collectors.toList());

            // 只有在userIds不为空时才执行删除
            if (!userIds.isEmpty()) {
                sysUserRoleService.remove(new QueryWrapper<SysUserRole>().in("user_id", userIds));
            }
        }

        // 删除机构下的用户
        sysUserService.remove(new QueryWrapper<SysUser>().eq("org_id", orgId));

        // 删除其它业务 TODO
    }


    @Override
    public Result<?> changeOrgStatus(String orgId, Integer status) {
        if (StringUtils.isBlank(orgId) || status == null) {
            throw new BusinessException("机构ID和状态不能为空");
        }

        SysOrg sysOrg = this.getById(orgId);
        if (sysOrg == null) {
            throw new BusinessException("机构不存在");
        }

        sysOrg.setStatus(status);
        boolean updateFlag = this.updateById(sysOrg);
        if (!updateFlag) {
            throw new BusinessException("机构状态切换失败");
        }
        return Result.success(status == 1 ? "机构启用成功" : "机构禁用成功");
    }

    @Override
    public List<SysOrg> queryAllEnableOrg() {
        LambdaQueryWrapper<SysOrg> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysOrg::getStatus, 1).ne(SysOrg::getOrgId, Constants.SYS_ORG_ID)
                .orderByDesc(SysOrg::getCreateTime);
        return this.baseMapper.selectList(queryWrapper);
    }
}
