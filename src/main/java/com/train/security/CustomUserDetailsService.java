package com.train.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.entity.SysRole;
import com.train.entity.SysUser;
import com.train.entity.SysUserRole;
import com.train.mapper.SysRoleMapper;
import com.train.mapper.SysUserMapper;
import com.train.mapper.SysUserRoleMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 自定义UserDetailsService，实现账号+机构的用户查询
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    /**
     * 加载用户信息
     * @param username 格式：account@orgId（账号@机构ID，确保唯一性）
     * @return 自定义UserDetails
     * @throws UsernameNotFoundException 用户不存在异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 拆分账号和机构ID
        String[] usernameArray = username.split("@", -1); // 使用-1确保保留空字符串部分
        if (usernameArray.length != 2) {
            throw new UsernameNotFoundException("登录标识格式错误，请使用 account@orgId 格式");
        }
        String account = usernameArray[0];
        String orgId = usernameArray[1];

        // 验证参数不为空
        if (account == null || account.trim().isEmpty() || orgId == null || orgId.trim().isEmpty()) {
            throw new UsernameNotFoundException("账号或机构ID不能为空");
        }

        // 验证orgId格式（防止SQL注入等攻击）
        if (!orgId.matches("\\d+")) {
            throw new UsernameNotFoundException("机构ID格式不正确");
        }

        // 2. 根据账号+机构ID查询用户（核心：确保同账号不同机构的用户唯一性）
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getAccount, account)
                        .eq(SysUser::getOrgId, orgId)
        );
        if (sysUser == null) {
            throw new UsernameNotFoundException("账号或机构不存在");
        }

        // 3. 查询用户关联的角色
        List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, sysUser.getUserId())
        );
        List<String> roleIds = userRoleList.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        // 4. 查询角色权限（封装为GrantedAuthority）
        List<SysRole> roleList = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getRoleId, roleIds)
        );
        List<GrantedAuthority> authorities = roleList.stream()
                .filter(Objects::nonNull)
                .map(role -> {
                    String roleId = role.getRoleId();
                    if (roleId == null) {
                        throw new IllegalArgumentException("Role ID cannot be null");
                    }
                    // 验证roleId只包含字母、数字和下划线
                    if (!roleId.matches("^[a-zA-Z0-9_]+$")) {
                        throw new IllegalArgumentException("Invalid role ID format: " + roleId);
                    }
                    return new SimpleGrantedAuthority(roleId);
                })
                .collect(Collectors.toList());

         // 5. 封装为自定义JwtUserDetails
        JwtUserDetails jwtUserDetails = new JwtUserDetails();
        jwtUserDetails.setUserId(sysUser.getUserId());
        jwtUserDetails.setAccount(sysUser.getAccount());
        jwtUserDetails.setUserName(sysUser.getUserName());
        jwtUserDetails.setOrgId(sysUser.getOrgId());
        jwtUserDetails.setIsSysAdmin(sysUser.getIsSysAdmin());
        jwtUserDetails.setAuthorities(authorities);
        jwtUserDetails.setPassword(sysUser.getPassword());
        jwtUserDetails.setStatus(sysUser.getStatus());

        return jwtUserDetails;
    }
}
