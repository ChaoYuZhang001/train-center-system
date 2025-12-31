package com.train.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.annotation.RequiresPermission;
import com.train.constant.ResultConstant;
import com.train.entity.SysMenu;
import com.train.entity.SysRole;
import com.train.entity.SysRoleMenu;
import com.train.entity.SysUserRole;
import com.train.exception.BusinessException;
import com.train.mapper.*;
import com.train.security.JwtUserDetails;
import com.train.util.RedisUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限拦截切面（修正优化版，兼容同账号不同机构）
 */
@Aspect
@Component
public class PermissionAspect {

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private SysOrgRoleMapper sysOrgRoleMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;
    // 在 PermissionAspect 中注入 Redis 工具类
    @Resource
    private RedisUtil redisUtil;
    private static final Logger log = LoggerFactory.getLogger(PermissionAspect.class);

    /**
     * 权限拦截切面：拦截带有@RequiresPermission注解的方法
     */
    @Before("@annotation(requiresPermission)")
    public void before(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        // 1. 获取所需权限标识
        String requiredPerm = requiresPermission.value();
        if (requiredPerm == null || requiredPerm.trim().isEmpty()) {
            throw new BusinessException("所需权限标识不能为空");
        }

        // 2. 获取当前登录用户的自定义详情（JwtUserDetails，含账号、机构ID等信息）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(ResultConstant.NO_AUTH_CODE,"未获取到用户认证信息，权限校验失败");
        }
        Object principal = authentication.getPrincipal();
        // 校验用户身份是否有效
        if (!(principal instanceof JwtUserDetails)) {
            throw new BusinessException(ResultConstant.NO_AUTH_CODE,"未获取到有效登录用户信息，权限校验失败");
        }
        JwtUserDetails currentUser = (JwtUserDetails) principal;

        // 3. 提取核心用户信息（避免重复拆分/查询）
        String pureAccount = currentUser.getAccount(); // 纯账号（非拼接字符串）
        String orgId = currentUser.getOrgId(); // 机构ID（关键：实现同账号不同机构隔离）
        Integer isSysAdmin = currentUser.getIsSysAdmin(); // 是否超级管理员
        Long userId = currentUser.getUserId(); // 用户ID

        // 校验核心信息非空
        if (pureAccount == null || pureAccount.trim().isEmpty()) {
            throw new BusinessException("用户账号信息为空，权限校验失败");
        }
        if (orgId == null || orgId.trim().isEmpty()) {
            throw new BusinessException("用户所属机构信息缺失，权限校验失败");
        }
        // 4. 获取当前请求的 Token 并检查是否在黑名单中
        String token = getCurrentToken();
        if (token != null && isTokenInBlacklist(token)) {
            throw new BusinessException(ResultConstant.NO_AUTH_CODE,"用户已登出，请重新登录");
        }
        // 4. 权限校验（核心逻辑）
        boolean hasPermission = false;
        // 4.1 超级管理员直接拥有所有权限
        if (1 == isSysAdmin) {
            hasPermission = true;
        } else {
            // 4.2 普通用户/机构管理员：校验角色关联的菜单权限
            hasPermission = checkUserMenuPermission(userId, orgId, requiredPerm);
        }

        // 5. 无权限则抛出异常并记录日志
        if (!hasPermission) {
            log.warn("用户[账号：{}，机构ID：{}，用户ID：{}]尝试访问无权限接口，所需权限：{}",
                    pureAccount, orgId, userId, requiredPerm);
            throw new BusinessException(ResultConstant.NO_AUTH_CODE,"没有访问该接口的权限，请联系管理员分配权限");
        }
    }
    /**
     * 检查Token是否在黑名单中
     */
    private boolean isTokenInBlacklist(String token) {
        try {
            String blacklistKey = "token:invalid:" + token;
            return redisUtil.hasKey(blacklistKey);
        } catch (Exception e) {
            log.error("检查Token黑名单异常：token={}", token, e);
            return false; // 异常时默认不在黑名单中，避免误拦截
        }
    }

    /**
     * 获取当前请求的Token
     */
    private String getCurrentToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                return token.substring(7);
            }
            return token;
        } catch (Exception e) {
            log.error("获取当前Token异常", e);
            return null;
        }
    }

    /**
     * 校验用户是否拥有指定菜单权限（结合机构ID，实现细粒度权限控制）
     * @param userId 用户ID
     * @param orgId  机构ID
     * @param requiredPerm  所需权限标识
     * @return true=有权限，false=无权限
     */
    private boolean checkUserMenuPermission(Long userId, String orgId, String requiredPerm) {
        try {
            // 参数校验
            if (userId == null || orgId == null || requiredPerm == null) {
                return false;
            }

            // 步骤1：根据用户ID查询关联的角色ID列表
            List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, userId)
            );
            if (userRoleList.isEmpty()) {
                return false; // 无关联角色，直接无权限
            }
            List<String> roleIds = userRoleList.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());

            // 步骤2：过滤当前机构绑定的角色（核心：同账号不同机构，角色仅局限于本机构）
            List<String> croleIds = sysOrgRoleMapper.selectRoleIdsByOrgId(orgId);
            if (croleIds.isEmpty()) {
                throw new BusinessException(ResultConstant.NO_AUTH_CODE,"当前机构无角色信息，请先为当前机构添加角色");
            }
            List<SysRole> orgRoleList = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>()
                            .in(SysRole::getRoleId, roleIds)
                            .eq(SysRole::getStatus, 1) // 仅查询启用状态的角色
            );
            if (orgRoleList.isEmpty()) {
                return false; // 该机构下无关联角色，无权限
            }
            List<String> orgRoleIds = orgRoleList.stream()
                    .map(SysRole::getRoleId)
                    .collect(Collectors.toList());

            // 步骤3：根据机构角色ID查询关联的菜单ID列表
            List<SysRoleMenu> roleMenuList = sysRoleMenuMapper.selectList(
                    new LambdaQueryWrapper<SysRoleMenu>()
                            .in(SysRoleMenu::getRoleId, orgRoleIds)
            );
            if (roleMenuList.isEmpty()) {
                return false; // 角色无关联菜单，无权限
            }
            List<Long> menuIds = roleMenuList.stream()
                    .map(SysRoleMenu::getMenuId)
                    .collect(Collectors.toList());

            // 步骤4：查询菜单是否包含所需权限标识
            SysMenu sysMenu = sysMenuMapper.selectOne(
                    new LambdaQueryWrapper<SysMenu>()
                            .in(SysMenu::getMenuId, menuIds)
                            .eq(SysMenu::getPerms, requiredPerm)
            );

            return sysMenu != null; // 存在该权限菜单即有权限
        } catch (Exception e) {
            log.error("校验用户权限异常：用户ID={}，机构ID={}，所需权限={}", userId, orgId, requiredPerm, e);
            return false; // 异常时默认无权限
        }
    }
}
