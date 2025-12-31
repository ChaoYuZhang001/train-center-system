package com.train.service;
import com.train.security.JwtUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class JwtUserDetailsTest {
    @Test
    public void testJwtUserDetails_Methods() {
        // 初始化
        JwtUserDetails user = new JwtUserDetails();
        user.setUserId(1L);
        user.setAccount("test");
        user.setUserName("测试");
        user.setOrgId("1001");
        user.setIsSysAdmin(0);
        user.setPassword("加密密码");
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        user.setAuthorities(authorities);
        user.setStatus(1); // 正常状态

        // 覆盖所有getter
        assertEquals(1L, user.getUserId());
        assertEquals("test", user.getAccount());
        assertEquals("测试", user.getUsername());
        assertEquals("1001", user.getOrgId());
        assertEquals(0, user.getIsSysAdmin());
        assertEquals("加密密码", user.getPassword());
        assertEquals(authorities, user.getAuthorities());
        assertEquals("test@1001", user.getUsername()); // 覆盖getUsername拼接逻辑

        // 覆盖状态方法
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled()); // status=1

        // 覆盖status=0（禁用）的isEnabled
        user.setStatus(0);
        assertFalse(user.isEnabled());
    }
}