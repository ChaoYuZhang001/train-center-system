package com.train.dto;

import com.train.security.JwtUserDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息DTO（脱敏，不包含密码）
 */
@Data
@Schema(description = "用户信息DTO（脱敏，不包含密码）")
public class UserInfoDTO implements Serializable {
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户账号")
    private String account;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "是否系统管理员", example = "0")
    private Integer isSysAdmin;

    @Schema(description = "用户状态", example = "1")
    private Integer status;

    @Schema(description = "账号是否未过期")
    private Boolean accountNonExpired;

    @Schema(description = "账号是否未锁定")
    private Boolean accountNonLocked;

    @Schema(description = "凭证是否未过期")
    private Boolean credentialsNonExpired;

    @Schema(description = "用户是否启用")
    private Boolean enabled;

    @Schema(description = "用户权限列表")
    private List<String> authorities;

    // 新增权限列表
    @Schema(description = "用户权限列表")
    private List<String> permissions;

    public UserInfoDTO() {}

    public UserInfoDTO(JwtUserDetails userDetails) {
        this.userId = userDetails.getUserId();
        this.account = userDetails.getAccount();
        this.username = userDetails.getUsername();
        this.orgId = userDetails.getOrgId();
        this.isSysAdmin = userDetails.getIsSysAdmin();
        this.status = userDetails.getStatus();
        this.accountNonExpired = userDetails.isAccountNonExpired();
        this.accountNonLocked = userDetails.isAccountNonLocked();
        this.credentialsNonExpired = userDetails.isCredentialsNonExpired();
        this.enabled = userDetails.isEnabled();
        this.authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }
}
