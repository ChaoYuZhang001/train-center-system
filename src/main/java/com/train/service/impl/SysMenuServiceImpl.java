package com.train.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.train.entity.SysMenu;
import com.train.entity.SysRoleMenu;
import com.train.mapper.SysMenuMapper;
import com.train.mapper.SysRoleMenuMapper;
import com.train.exception.BusinessException;
import com.train.util.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单业务实现类
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public IPage<SysMenu> queryMenuPage(Page<SysMenu> page, String menuName, Integer type) {
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        // 模糊查询 + 类型筛选
        queryWrapper.like(StringUtils.isNotBlank(menuName), SysMenu::getMenuName, menuName)
                .eq(type != null, SysMenu::getType, type)
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort)
                .orderByDesc(SysMenu::getCreateTime);
        return this.baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public List<SysMenu> queryMenuTree() {
        // 1. 查询所有启用的菜单
        List<SysMenu> menuList = this.baseMapper.selectAllEnableMenu();
        // 2. 构建树形结构
        return this.buildMenuTree(menuList, 0L);
    }

    @Override
    public Result<?> addMenu(SysMenu sysMenu) {
        // 1. 校验父级菜单合法性
        if (sysMenu.getParentId() != null && sysMenu.getParentId() != 0) {
            SysMenu parentMenu = this.getById(sysMenu.getParentId());
            if (parentMenu == null) {
                throw new BusinessException("父级菜单不存在，请选择合法父级菜单");
            }
            // 按钮类型菜单的父级不能是目录
            if (sysMenu.getType() == 2 && parentMenu.getType() == 0) {
                throw new BusinessException("按钮类型菜单的父级不能是目录，请选择菜单作为父级");
            }
        }
        // 2. 校验同一父级下菜单名称唯一性
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysMenu::getMenuName, sysMenu.getMenuName())
                .eq(SysMenu::getParentId, sysMenu.getParentId() == null ? 0 : sysMenu.getParentId());
        if (this.baseMapper.exists(queryWrapper)) {
            throw new BusinessException("同一父级下已存在同名菜单，请更换");
        }
        // 3. 补全默认值
        if (sysMenu.getParentId() == null) {
            sysMenu.setParentId(0L); // 默认一级菜单
        }
        if (sysMenu.getType() == null) {
            throw new BusinessException("菜单类型不能为空（0=目录，1=菜单，2=按钮）");
        }
        if (sysMenu.getSort() == null) {
            sysMenu.setSort(0);
        }
        if (sysMenu.getStatus() == null) {
            sysMenu.setStatus(1); // 默认启用
        }
        sysMenu.setCreateTime(LocalDateTime.now());
        // 4. 保存菜单
        boolean saveFlag = this.save(sysMenu);
        if (!saveFlag) {
            throw new BusinessException("菜单新增失败");
        }
        return Result.success("菜单新增成功");
    }

    @Override
    public Result<?> editMenu(SysMenu sysMenu) {
        if (sysMenu.getMenuId() == null) {
            throw new BusinessException("菜单ID不能为空");
        }
        // 1. 校验菜单是否存在
        SysMenu oldMenu = this.getById(sysMenu.getMenuId());
        if (oldMenu == null) {
            throw new BusinessException("菜单不存在");
        }
        // 2. 校验父级合法性（不能选择自身作为父级，不能选择子菜单作为父级）
        Long parentId = sysMenu.getParentId() == null ? 0 : sysMenu.getParentId();
        if (sysMenu.getMenuId().equals(parentId)) {
            throw new BusinessException("不能选择自身作为父级菜单");
        }
        // 校验是否选择子菜单作为父级
        List<SysMenu> childMenuList = this.queryChildMenuList(sysMenu.getMenuId());
        if (childMenuList.stream().anyMatch(menu -> menu.getMenuId().equals(parentId))) {
            throw new BusinessException("不能选择子菜单作为父级菜单");
        }
        // 3. 校验同一父级下名称唯一性（排除自身）
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysMenu::getMenuName, sysMenu.getMenuName())
                .eq(SysMenu::getParentId, parentId)
                .ne(SysMenu::getMenuId, sysMenu.getMenuId());
        if (this.baseMapper.exists(queryWrapper)) {
            throw new BusinessException("同一父级下已存在同名菜单，请更换");
        }
        // 4. 按钮类型菜单的父级不能是目录
        if (sysMenu.getType() == 2) {
            SysMenu parentMenu = this.getById(parentId);
            if (parentMenu != null && parentMenu.getType() == 0) {
                throw new BusinessException("按钮类型菜单的父级不能是目录，请选择菜单作为父级");
            }
        }
        // 5. 更新菜单
        sysMenu.setCreateTime(oldMenu.getCreateTime()); // 不修改创建时间
        boolean updateFlag = this.updateById(sysMenu);
        if (!updateFlag) {
            throw new BusinessException("菜单编辑失败");
        }
        return Result.success("菜单编辑成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteMenu(Long menuId) {
        if (menuId == null) {
            throw new BusinessException("菜单ID不能为空");
        }
        // 1. 校验是否有子菜单，有子菜单不可删除
        List<SysMenu> childMenuList = this.queryChildMenuList(menuId);
        if (!childMenuList.isEmpty()) {
            throw new BusinessException("该菜单存在子菜单，不可直接删除，请先删除子菜单");
        }
        // 2. 校验是否被角色关联，有关联不可删除
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.eq(SysRoleMenu::getMenuId, menuId);
        if (sysRoleMenuMapper.exists(roleMenuWrapper)) {
            throw new BusinessException("该菜单已被角色关联，不可删除");
        }
        // 3. 删除菜单
        boolean deleteFlag = this.removeById(menuId);
        if (!deleteFlag) {
            throw new BusinessException("菜单删除失败");
        }
        return Result.success("菜单删除成功");
    }

    @Override
    public Result<?> changeMenuStatus(Long menuId, Integer status) {
        if (menuId == null || status == null) {
            throw new BusinessException("菜单ID和状态不能为空");
        }
        SysMenu sysMenu = this.getById(menuId);
        if (sysMenu == null) {
            throw new BusinessException("菜单不存在");
        }
        // 若修改的是目录，同步更新所有子菜单状态
        if (sysMenu.getType() == 0) {
            List<SysMenu> childMenuList = this.queryChildMenuList(menuId);
            if (!childMenuList.isEmpty()) {
                List<SysMenu> updateMenuList = childMenuList.stream()
                        .peek(menu -> menu.setStatus(status))
                        .collect(Collectors.toList());
                this.updateBatchById(updateMenuList);
            }
        }
        // 更新当前菜单状态
        sysMenu.setStatus(status);
        boolean updateFlag = this.updateById(sysMenu);
        if (!updateFlag) {
            throw new BusinessException("菜单状态切换失败");
        }
        return Result.success(status == 1 ? "菜单启用成功" : "菜单禁用成功");
    }

    @Override
    public List<SysMenu> queryPermMenuTreeByRoleIds(List<String> roleIds) {
        // 1. 查询角色关联的菜单
        List<SysMenu> menuList = this.baseMapper.selectMenuListByRoleIds(roleIds);
        // 2. 构建树形结构
        return this.buildMenuTree(menuList, 0L);
    }

    /**
     * 构建菜单树形结构（递归）
     * @param menuList 扁平菜单列表
     * @param parentId 父级菜单ID
     * @return 树形菜单列表
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> menuList, Long parentId) {
        List<SysMenu> treeMenuList = new ArrayList<>();
        for (SysMenu menu : menuList) {
            if (parentId.equals(menu.getParentId())) {
                // 递归查询子菜单
                List<SysMenu> childMenuList = this.buildMenuTree(menuList, menu.getMenuId());
                menu.setChildren(childMenuList);
                treeMenuList.add(menu);
            }
        }
        return treeMenuList;
    }

    /**
     * 查询子菜单列表（递归查询所有层级子菜单）
     * @param menuId 菜单ID
     * @return 子菜单列表
     */
    private List<SysMenu> queryChildMenuList(Long menuId) {
        List<SysMenu> childMenuList = new ArrayList<>();
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysMenu::getParentId, menuId);
        List<SysMenu> firstLevelChild = this.baseMapper.selectList(queryWrapper);
        if (!firstLevelChild.isEmpty()) {
            childMenuList.addAll(firstLevelChild);
            // 递归查询子菜单的子菜单
            for (SysMenu child : firstLevelChild) {
                childMenuList.addAll(queryChildMenuList(child.getMenuId()));
            }
        }
        return childMenuList;
    }
}