package com.train.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.train.entity.SysRole;
import com.train.entity.SysRoleMenu;
import com.train.entity.SysUser;
import com.train.entity.SysUserRole;
import com.train.exception.BusinessException;
import com.train.mapper.SysOrgRoleMapper;
import com.train.mapper.SysRoleMapper;
import com.train.mapper.SysRoleMenuMapper;
import com.train.mapper.SysUserRoleMapper;
import com.train.service.ISysRoleService;
import com.train.service.SysRoleMenuService;
import com.train.util.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 角色业务实现类
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;
    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Resource
    private SysOrgRoleMapper sysOrgRoleMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    private static final DateTimeFormatter ROLE_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger dailySequence = new AtomicInteger(1);
    private static final ReentrantLock sequenceLock = new ReentrantLock();
    private static String currentDateCache = LocalDateTime.now().format(ROLE_ID_FORMATTER);

    @Override
    public IPage<SysRole> queryRolePage(Page<SysRole> page, String roleName, String orgId, String currentOrgId, Integer isSysAdmin) {
        // 参数验证
        validateOrgId(currentOrgId);
        if (StringUtils.isNotBlank(orgId)) {
            validateOrgId(orgId);
        }

        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(roleName), SysRole::getRoleName, roleName)
                .orderByAsc(SysRole::getSort)
                .orderByDesc(SysRole::getCreateTime);
        // 权限隔离：超级管理员查所有，机构管理员查本机构
        if (isSysAdmin != 1) {
            List<String> roleIds = sysOrgRoleMapper.selectRoleIdsByOrgId(currentOrgId);
            queryWrapper.in(SysRole::getRoleId, roleIds);
        } else if (StringUtils.isNotBlank(orgId)) {
            // 超级管理员可按机构筛选
            List<String> roleIds = sysOrgRoleMapper.selectRoleIdsByOrgId(orgId);
            queryWrapper.in(SysRole::getRoleId, roleIds);
        }
        return this.baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> addRole(SysUser currentUser, SysRole sysRole, List<Long> menuIds) {
        String currentOrgId = currentUser.getOrgId();
        // 参数验证
        validateOrgId(currentOrgId);

        // 1. 校验角色名称唯一性（同一机构内）
        List<String> roleIds = sysOrgRoleMapper.selectRoleIdsByOrgId(currentOrgId);
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getRoleName, sysRole.getRoleName())
                .in(SysRole::getRoleId, roleIds);
        if (this.baseMapper.exists(queryWrapper)) {
            throw new BusinessException("该机构下已存在同名角色，请更换");
        }

        //角色ID	字符串	20	是	唯一标识，格式：ROLE时间戳 + 当日序号001-999
        try {
            String datePart = "ROLE" + LocalDateTime.now().format(ROLE_ID_FORMATTER);
            String sequenceStr = generateSequenceNumber();
            sysRole.setRoleId(datePart + sequenceStr);
        } catch (Exception e) {
            throw new BusinessException(500,"生成角色ID失败" + e.getMessage());
        }

        // 2. 补全默认值
        if (sysRole.getRoleType() == null) {
            sysRole.setRoleType(1); // 默认机构级角色
        }
        if (sysRole.getStatus() == null) {
            sysRole.setStatus(1); // 默认启用
        }
        if (sysRole.getCanDelete() == null) {
            sysRole.setCanDelete(1); // 默认可删除
        }
        if (sysRole.getSort() == null) {
            sysRole.setSort(0);
        }
        if (sysRole.getCreateBy() == null) {
            sysRole.setCreateBy(currentUser.getAccount());
        }
        sysRole.setCreateTime(LocalDateTime.now());

        // 3. 保存角色
        boolean saveFlag = this.save(sysRole);
        if (!saveFlag) {
            throw new BusinessException("角色新增失败");
        }

        // 4. 分配菜单权限
        this.saveRoleMenu(sysRole.getRoleId(), menuIds);
        return Result.success("角色新增成功",sysRole);
    }

    private String generateSequenceNumber() {
        sequenceLock.lock();
        try {
            String today = LocalDateTime.now().format(ROLE_ID_FORMATTER);

            // 检查是否跨日期，如果是则重置序列号
            if (!currentDateCache.equals(today)) {
                dailySequence.set(1);
                currentDateCache = today;
            }

            int sequence = dailySequence.getAndIncrement();

            // 检查序列号是否超过最大值
            if (sequence > 999) {
                dailySequence.set(1);
                sequence = dailySequence.getAndIncrement();
            }

            return String.format("%03d", sequence);
        } finally {
            sequenceLock.unlock();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> editRole(String currentOrgId, SysRole sysRole, List<Long> menuIds) {
        if (StringUtils.isBlank(sysRole.getRoleId())) {
            throw new BusinessException("角色ID不能为空");
        }
        validateOrgId(currentOrgId);

        // 1. 校验角色是否可修改（系统超级管理员角色不可修改）
        SysRole oldRole = this.getById(sysRole.getRoleId());
        if (oldRole == null) {
            throw new BusinessException("角色不存在");
        }
        if (oldRole.getRoleType() == 0 && oldRole.getCanDelete() == 0) {
            throw new BusinessException("系统超级管理员角色不可修改");
        }
        // 2. 校验角色名称唯一性（排除自身）
        List<String> roleIds = sysOrgRoleMapper.selectRoleIdsByOrgId(currentOrgId);

        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getRoleName, sysRole.getRoleName())
                .in(SysRole::getRoleId, roleIds)
                .ne(SysRole::getRoleId, sysRole.getRoleId());
        if (this.baseMapper.exists(queryWrapper)) {
            throw new BusinessException("该机构下已存在同名角色，请更换");
        }
        // 3. 更新角色
        boolean updateFlag = this.updateById(sysRole);
        if (!updateFlag) {
            throw new BusinessException("角色编辑失败");
        }
        // 4. 更新菜单权限
        this.saveRoleMenu(sysRole.getRoleId(), menuIds);
        return Result.success("角色编辑成功",sysRole);
    }

    @Override
    public Result<?> deleteRole(String roleId) {
        if (StringUtils.isBlank(roleId)) {
            throw new BusinessException("角色ID不能为空");
        }
        // 1. 校验角色是否可删除
        SysRole sysRole = this.getById(roleId);
        if (sysRole == null) {
            throw new BusinessException("角色不存在");
        }
        if (sysRole.getCanDelete() == 0) {
            throw new BusinessException("该角色不可删除");
        }
        // 2. 校验是否有用户关联该角色
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getRoleId, roleId);
        if (sysUserRoleMapper.exists(userRoleWrapper)) {
            throw new BusinessException("该角色已关联用户，不可直接删除");
        }
        // 3. 删除角色及关联权限
        this.baseMapper.deleteById(roleId);
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.eq(SysRoleMenu::getRoleId, roleId);
        sysRoleMenuMapper.delete(roleMenuWrapper);
        return Result.success("角色删除成功");
    }

    @Override
    public Result<?> changeRoleStatus(String roleId, Integer status) {
        if (StringUtils.isBlank(roleId) || status == null) {
            throw new BusinessException("角色ID和状态不能为空");
        }
        SysRole sysRole = this.getById(roleId);
        if (sysRole == null) {
            throw new BusinessException("角色不存在");
        }
        // 系统超级管理员角色不可修改状态
        if (sysRole.getRoleType() == 0 && sysRole.getCanDelete() == 0) {
            throw new BusinessException("系统超级管理员角色不可修改状态");
        }
        sysRole.setStatus(status);
        boolean updateFlag = this.updateById(sysRole);
        if (!updateFlag) {
            throw new BusinessException("角色状态切换失败");
        }
        return Result.success(status == 1 ? "角色启用成功" : "角色禁用成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignRolePerm(String roleId, List<Long> menuIds) {
        if (StringUtils.isBlank(roleId)) {
            throw new BusinessException("角色ID不能为空");
        }
        // 校验角色是否可分配权限
        SysRole sysRole = this.getById(roleId);
        if (sysRole == null) {
            throw new BusinessException("角色不存在");
        }
        if (sysRole.getRoleType() == 0 && sysRole.getCanDelete() == 0) {
            throw new BusinessException("系统超级管理员角色不可修改权限");
        }
        // 更新菜单权限
        this.saveRoleMenu(roleId, menuIds);
        return Result.success("角色权限分配成功");
    }

    @Override
    public List<SysRole> queryRoleListByOrgId(String orgId) {
        validateOrgId(orgId);
        List<String> roleIds = sysOrgRoleMapper.selectRoleIdsByOrgId(orgId);

        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysRole::getRoleId, roleIds)
                .eq(SysRole::getStatus, 1)
                .eq(SysRole::getCanDelete, 1)
                .orderByAsc(SysRole::getSort);
        return this.baseMapper.selectList(queryWrapper);
    }

    /**
     * 保存角色-菜单关联关系（先删后加）
     */
    private void saveRoleMenu(String roleId, List<Long> menuIds) {
        // 1. 删除原有关联
        LambdaQueryWrapper<SysRoleMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenu::getRoleId, roleId);
        sysRoleMenuMapper.delete(queryWrapper);
        // 2. 新增新关联
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        List<SysRoleMenu> roleMenuList = menuIds.stream()
                .map(menuId -> {
                    SysRoleMenu sysRoleMenu = new SysRoleMenu();
                    sysRoleMenu.setRoleId(roleId);
                    sysRoleMenu.setMenuId(menuId);
                    return sysRoleMenu;
                }).collect(Collectors.toList());
        sysRoleMenuService.saveBatch(roleMenuList);
    }

    private void validateOrgId(String orgId) {
        if (StringUtils.isBlank(orgId)) {
            throw new BusinessException("机构ID不能为空");
        }
        // 可以根据实际需求添加更严格的验证规则
        if (orgId.length() > 50) {
            throw new BusinessException("机构ID格式不正确");
        }
    }
}
