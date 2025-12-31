package com.train.service;
import com.train.entity.SysUser;
import com.train.exception.BusinessException;
import com.train.mapper.SysUserMapper;
import com.train.service.impl.SysUserServiceImpl;
import com.train.util.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SysUserServiceImplTest {
    @Mock
    private SysUserMapper userMapper;
    @InjectMocks
    private SysUserServiceImpl userService;

    @Test
    public void testResetUserPwd_UserIdNull() {
        // 覆盖userId == null分支
        assertThrows(BusinessException.class, () -> userService.resetUserPwd(null, "123456"));
    }

    @Test
    public void testResetUserPwd_NewPasswordBlank() {
        // 覆盖newPassword为空分支
        assertThrows(BusinessException.class, () -> userService.resetUserPwd(1L, ""));
    }

    @Test
    public void testResetUserPwd_UserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null); // 覆盖用户不存在分支
        assertThrows(BusinessException.class, () -> userService.resetUserPwd(1L, "123456"));
    }

    @Test
    public void testResetUserPwd_IsSysAdmin() {
        SysUser admin = new SysUser();
        admin.setIsSysAdmin(1); // 超级管理员
        when(userMapper.selectById(1L)).thenReturn(admin);
        assertThrows(BusinessException.class, () -> userService.resetUserPwd(1L, "123456"));
    }

    @Test
    public void testResetUserPwd_UpdateFail() {
        SysUser user = new SysUser();
        user.setIsSysAdmin(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(0); // 更新失败
        assertThrows(BusinessException.class, () -> userService.resetUserPwd(1L, "123456"));
    }

    @Test
    public void testResetUserPwd_Success() {
        SysUser user = new SysUser();
        user.setIsSysAdmin(0);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(1); // 更新成功
        Result<?> result = userService.resetUserPwd(1L, "123456");
        assertEquals("密码重置成功", result.getMessage());
        // 验证密码已加密
        assertNotNull(user.getPassword());
        assertNotEquals("123456", user.getPassword());
    }
}