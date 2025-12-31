package com.train.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 自定义UserDetails，存储用户核心信息（含机构ID）
 */
@Data
public class JwtUserDetails implements UserDetails {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 所属机构ID（关键：区分同账号不同机构）
     */
    private String orgId;

    /**
     * 是否超级管理员（0=否，1=是）
     */
    private Integer isSysAdmin;

    /**
     * 密码（加密后）
     */
    private String password;

    /**
     * 角色权限列表（如ROLE_ADMIN、sys:org:list）
     */
    private List<GrantedAuthority> authorities;
    // 新增权限列表
    private List<String> permissions;
    /**
     * 用户状态（1=正常，0=禁用）
     */
    private Integer status;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    // 注意：Spring Security 默认用 username 作为登录标识，这里返回 账号@机构ID 确保唯一性
    @Override
    public String getUsername() {
        return this.account + "@" + this.orgId;
    }

    // 以下默认返回true，可根据业务调整
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 校验用户状态是否正常
    @Override
    public boolean isEnabled() {
        return this.status == 1;
    }
}