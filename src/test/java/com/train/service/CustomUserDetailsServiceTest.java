package com.train.service;

import com.train.entity.SysRole;
import com.train.entity.SysUser;
import com.train.entity.SysUserRole;
import com.train.mapper.SysRoleMapper;
import com.train.mapper.SysUserMapper;
import com.train.mapper.SysUserRoleMapper;
import com.train.security.CustomUserDetailsService;
import com.train.security.JwtUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Collections;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    public void testLoadUserByUsername_FormatError() {
        // 覆盖格式错误（无@、拆分后长度≠2）
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("test"));
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("test@org@extra"));
    }

    @Test
    public void testLoadUserByUsername_AccountOrOrgIdBlank() {
        // 账号为空
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("@1001"));
        // 机构ID为空
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("test@"));
    }

    @Test
    public void testLoadUserByUsername_OrgIdInvalid() {
        // orgId非数字（覆盖正则校验）
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("test@org1001"));
    }

    @Test
    public void testLoadUserByUsername_UserNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null); // 用户不存在
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("test@1001"));
    }

    @Test
    public void testLoadUserByUsername_NoRoles() {
        // 无角色关联
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setAccount("test");
        user.setOrgId("1001");
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userRoleMapper.selectList(any())).thenReturn(Collections.emptyList()); // 无角色

        // 执行（应正常返回，权限为空）
        JwtUserDetails details = (JwtUserDetails) userDetailsService.loadUserByUsername("test@1001");
        assertTrue(details.getAuthorities().isEmpty());
    }

    @Test
    public void testLoadUserByUsername_RoleIdInvalid() {
        // 角色ID含特殊字符（覆盖正则校验）
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setAccount("test");
        user.setOrgId("1001");
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId("role@1"); // 含@，非法
        when(userRoleMapper.selectList(any())).thenReturn(Collections.singletonList(userRole));

        SysRole role = new SysRole();
        role.setRoleId("role@1");
        when(roleMapper.selectList(any())).thenReturn(Collections.singletonList(role));

        // 覆盖角色ID格式校验异常
        assertThrows(IllegalArgumentException.class, () -> userDetailsService.loadUserByUsername("test@1001"));
    }

    @Test
    public void testLoadUserByUsername_Success() {
        // 正常流程
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setAccount("test");
        user.setOrgId("1001");
        user.setUserName("测试");
        user.setPassword("加密密码");
        user.setIsSysAdmin(0);
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId("ROLE_ADMIN");
        when(userRoleMapper.selectList(any())).thenReturn(Collections.singletonList(userRole));

        SysRole role = new SysRole();
        role.setRoleId("ROLE_ADMIN");
        when(roleMapper.selectList(any())).thenReturn(Collections.singletonList(role));

        JwtUserDetails details = (JwtUserDetails) userDetailsService.loadUserByUsername("test@1001");
        assertEquals(1L, details.getUserId());
        assertEquals("test@1001", details.getUsername());
        assertEquals("加密密码", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }
}