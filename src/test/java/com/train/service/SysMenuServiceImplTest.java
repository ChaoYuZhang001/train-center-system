package com.train.service;

import com.train.entity.SysMenu;
import com.train.exception.BusinessException;
import com.train.mapper.SysMenuMapper;
import com.train.mapper.SysRoleMenuMapper;
import com.train.util.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SysMenuServiceImplTest {
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @InjectMocks
    private com.train.service.SysMenuServiceImpl menuService;

    @Test
    public void testDeleteMenu_MenuIdNull() {
        assertThrows(BusinessException.class, () -> menuService.deleteMenu(null));
    }

    @Test
    public void testDeleteMenu_HasChildMenu() {
        // 有子菜单（覆盖递归查询子菜单分支）
        when(menuMapper.selectList(any())).thenReturn(Arrays.asList(new SysMenu())); // 模拟存在子菜单
        assertThrows(BusinessException.class, () -> menuService.deleteMenu(1L));
    }

    @Test
    public void testDeleteMenu_RoleRelated() {
        // 被角色关联
        when(menuMapper.selectList(any())).thenReturn(Collections.emptyList()); // 无子菜单
        when(roleMenuMapper.exists(any())).thenReturn(true); // 被角色关联
        assertThrows(BusinessException.class, () -> menuService.deleteMenu(1L));
    }

    @Test
    public void testDeleteMenu_DeleteFail() {
        when(menuMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(roleMenuMapper.exists(any())).thenReturn(false);
        when(menuMapper.deleteById(1L)).thenReturn(0); // 删除失败
        assertThrows(BusinessException.class, () -> menuService.deleteMenu(1L));
    }

    @Test
    public void testDeleteMenu_Success() {
        when(menuMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(roleMenuMapper.exists(any())).thenReturn(false);
        when(menuMapper.deleteById(1L)).thenReturn(1); // 删除成功
        Result<?> result = menuService.deleteMenu(1L);
        assertEquals("菜单删除成功", result.getMessage());
    }
}