package com.train.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.train.constant.Constants;
import com.train.dto.UserInfoDTO;
import com.train.entity.SysMenu;
import com.train.entity.SysRoleMenu;
import com.train.entity.SysUser;
import com.train.entity.SysUserRole;
import com.train.mapper.SysUserMapper;
import com.train.security.JwtUserDetails;
import com.train.service.AuthService;
import com.train.service.ISysMenuService;
import com.train.service.SysRoleMenuService;
import com.train.service.SysUserRoleService;
import com.train.util.AESUtil;
import com.train.util.JwtTokenUtil;
import com.train.util.RedisUtil;
import com.train.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 认证授权业务实现类（修正优化版，兼容同账号不同机构）
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMenuService sysRoleMenuService;

    @Resource
    private SysUserRoleService sysUserRoleService;

    @Resource
    private ISysMenuService sysMenuService;



    // 密码错误次数前缀
    private static final String LOGIN_ERROR_COUNT_PREFIX = "login:error:count:";
    // 密码错误时间前缀
    private static final String LOGIN_ERROR_TIME_PREFIX = "login:error:time:";
    // 锁定时间（1小时）
    private static final int LOCK_DURATION = 3600;
    // 最大错误次数
    private static final int MAX_ERROR_COUNT = 5;
    // 提示次数
    private static final int WARNING_COUNT = 4;

    /**
     * 构建Redis键名，防止键名注入
     */
    private String buildRedisKey(String prefix, String account, String orgId) {
        // 验证输入参数，防止键名注入
        if (account == null || orgId == null) {
            throw new IllegalArgumentException("Account and orgId cannot be null");
        }
        // 更严格的验证：只允许字母数字下划线点和@符号
        if (!account.matches("^[a-zA-Z0-9_@.-]+$") || !orgId.matches("^[a-zA-Z0-9_@.-]+$")) {
            throw new IllegalArgumentException("Invalid characters in account or orgId");
        }
        return prefix + account + ":" + orgId;
    }

    /**
     * 构建用户锁定键名
     */
    private String buildLockKey(String account, String orgId) {
        return buildRedisKey("user:locked:", account, orgId);
    }

    /**
     * 构建用户信息键名
     */
    private String buildUserInfoKey(String token) {
        return "user:info:" + token;
    }

    /**
     * 构建Token黑名单键名
     */
    private String buildBlacklistKey(String token) {
        return "token:invalid:" + token;
    }

    /**
     * 构建错误计数键名
     */
    private String buildErrorCountKey(String uniqueUsername) {
        return LOGIN_ERROR_COUNT_PREFIX + uniqueUsername;
    }

    /**
     * 构建错误时间键名
     */
    private String buildErrorTimeKey(String uniqueUsername) {
        return LOGIN_ERROR_TIME_PREFIX + uniqueUsername;
    }

    /**
     * 带机构ID登录（核心方法，支持同账号不同机构区分）
     */
    @Override
    public Result<Map<String, Object>> login(String account, String password, String orgId) {

        // 1. 输入验证
        if (account == null || account.trim().isEmpty()) {
            return Result.error("账户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (orgId == null ) {
            orgId = Constants.SYS_ORG_ID;
//            return Result.error("组织ID无效（需传入有效ID）"); TODO
        }

        // 防止SQL注入及长度溢出
        if (account.length() > 50 || password.length() > 100) {
            return Result.error("账户名或密码长度超出限制（账户最多50位，密码最多100位）");
        }

        // 检查用户是否被锁定
        String lockKey = buildLockKey(account, orgId);
        if (redisUtil.hasKey(lockKey)) {
            return Result.error("本账号已锁定，请联系管理员重置密码");
        }

        try {

            // 3. 构建唯一认证标识：account@orgId（关键！匹配CustomUserDetailsService）
            String uniqueUsername = account + "@" + orgId;

            // 4. 对接收到的AES加密密码进行解密
            String decryptedPassword;
            try {
                decryptedPassword = AESUtil.desEncrypt(password);
            } catch (Exception e) {
                logger.error("密码解密失败，账户: {}", account, e);
                return Result.error("登录失败，请联系管理员");
            }

//            // 4. 构建用户名密码认证令牌（传入唯一标识，而非纯账号）
//            UsernamePasswordAuthenticationToken authenticationToken =
//                    new UsernamePasswordAuthenticationToken(uniqueUsername, decryptedPassword);
//
//            // 5. 执行认证（失败会抛出异常，如用户不存在、密码错误等）
//            Authentication authentication = authenticationManager.authenticate(authenticationToken);
//
//            // 6. 获取自定义用户详情（强转为JwtUserDetails，获取完整用户信息）
//            JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
//
//            // 7. 生成JWT Token（无需手动添加orgId，userDetails已包含，由JwtTokenUtil统一处理）
//            String token = jwtTokenUtil.generateToken(userDetails);
//
//            // 8. 将用户信息缓存到Redis
//            String userInfoKey = buildUserInfoKey(token);
//            redisUtil.set(userInfoKey, userDetails, jwtTokenUtil.getExpiration(), TimeUnit.SECONDS);
////
//            // 9. 封装返回结果（包含token信息和脱敏的用户信息）
//            Map<String, Object> resultMap = new HashMap<>(4);
//            resultMap.put("accessToken", token);
//            resultMap.put("tokenType", "Bearer");
//            resultMap.put("expiresIn", jwtTokenUtil.getExpirationFromToken(token).getTime() - System.currentTimeMillis());
//            // 创建脱敏的用户信息（不包含密码）
//            UserInfoDTO userInfo = new UserInfoDTO(userDetails);
//            List<String> permissions = getUserPermissions(userDetails.getUserId());
//            userInfo.setPermissions(permissions);
//            resultMap.put("userInfo", userInfo);
//
//            // 登录成功，清除错误次数
//            String errorCountKey = buildErrorCountKey(uniqueUsername);
//            String errorTimeKey = buildErrorTimeKey(uniqueUsername);
//            redisUtil.delete(errorCountKey);
//            redisUtil.delete(errorTimeKey);
//            return Result.success("登录成功", resultMap);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(uniqueUsername, decryptedPassword);
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            JwtUserDetails userDetails = (JwtUserDetails) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(userDetails);

            // 关键：使用JwtTokenUtil的方法构建用户信息Key，并设置过期时间与JWT一致
            String userInfoKey = jwtTokenUtil.buildUserInfoKey(token);
            redisUtil.set(userInfoKey, userDetails, jwtTokenUtil.getExpiration(), TimeUnit.SECONDS);

            Map<String, Object> resultMap = new HashMap<>(4);
            resultMap.put("accessToken", token);
            resultMap.put("tokenType", "Bearer");
            resultMap.put("expiresIn", jwtTokenUtil.getExpiration());
            UserInfoDTO userInfo = new UserInfoDTO(userDetails);
            List<String> permissions = getUserPermissions(userDetails.getUserId());
            userInfo.setPermissions(permissions);
            resultMap.put("userInfo", userInfo);

            // 清除错误次数
            String errorCountKey = buildErrorCountKey(uniqueUsername);
            String errorTimeKey = buildErrorTimeKey(uniqueUsername);
            redisUtil.delete(errorCountKey);
            redisUtil.delete(errorTimeKey);
            return Result.success("登录成功", resultMap);
        } catch (AuthenticationException e) {
            // 认证失败：密码错误、用户禁用等
            String uniqueUsername = account + "@" + orgId;
            String errorCountKey = buildErrorCountKey(uniqueUsername);
            String errorTimeKey = buildErrorTimeKey(uniqueUsername);

            // 获取当前错误次数
            Object countObj = redisUtil.get(errorCountKey);
            int errorCount = countObj != null ? (Integer) countObj : 0;
            errorCount++;

            // 设置错误时间（如果还没有设置）
            if (!redisUtil.hasKey(errorTimeKey)) {
                redisUtil.set(errorTimeKey, System.currentTimeMillis(), LOCK_DURATION, TimeUnit.SECONDS);
            }

            // 更新错误次数
            redisUtil.set(errorCountKey, errorCount, LOCK_DURATION, TimeUnit.SECONDS);

            // 检查是否需要锁定用户 - 超级管理员不受限制
            SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                    .eq("account", account)
                    .eq("org_id", orgId)
            );

            // 如果是超级管理员，不执行锁定逻辑
            if (user != null && user.getIsSysAdmin() != null && user.getIsSysAdmin() == 1) {
                // 超级管理员不锁定，但记录错误次数
                logger.warn("超级管理员登录失败: {}@{} 错误次数: {}", account, orgId, errorCount);
                return Result.error("账户名、密码错误，或用户已被禁用");
            }

            // 检查是否需要锁定用户
            if (errorCount >= MAX_ERROR_COUNT) {
                // 锁定用户1小时
                String lockKeyToSet = buildLockKey(account, orgId.toString());
                redisUtil.set(lockKeyToSet, "locked", (long) LOCK_DURATION, TimeUnit.SECONDS);
                redisUtil.delete(errorCountKey); // 清除错误计数
                logger.info("用户被锁定: {}@{}", account, orgId);
                return Result.error("本账号已锁定，请联系管理员重置密码");
            } else if (errorCount == WARNING_COUNT) {
                // 第4次错误时提示
                return Result.error("连续5次输入错误密码，将锁定本账号");
            } else {
                logger.warn("登录失败，错误次数: {}@{} 错误次数: {}", account, orgId, errorCount);
                return Result.error("账户名、密码错误，或用户已被禁用");
            }
        } catch (Exception e) {
            // 其他异常：记录日志，对外返回通用提示
            logger.error("登录过程发生异常", e);
            return Result.error("登录失败，请联系管理员");
        }
    }
     /**
     * 获取用户的所有权限标识
     */
    private List<String> getUserPermissions(Long userId) {
        // 通过用户ID查询关联的角色
        List<SysUserRole> userRoles = sysUserRoleService.list(
                new QueryWrapper<SysUserRole>().eq("user_id", userId)
        );

        if (userRoles.isEmpty()) {
            return Collections.emptyList(); // 修复：使用 Collections.emptyList()
        }

        List<String> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        // 查询角色关联的菜单权限
        List<SysRoleMenu> roleMenus = sysRoleMenuService.list(
                new QueryWrapper<SysRoleMenu>().in("role_id", roleIds)
        );

        if (roleMenus.isEmpty()) {
            return Collections.emptyList(); // 修复：使用 Collections.emptyList()
        }

        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());

        // 查询具体的权限标识
        List<SysMenu> menus = sysMenuService.list(
                new QueryWrapper<SysMenu>()
                        .in("menu_id", menuIds)
                        .isNotNull("perms") // 确保权限标识不为null
                        .ne("perms", "") // 确保权限标识不为空字符串
        );

        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(Objects::nonNull) // 过滤null权限
                .filter(perms -> !perms.trim().isEmpty()) // 过滤空字符串权限
                .distinct() // 去除重复权限
                .sorted() // 排序权限列表
                .collect(Collectors.toList());
    }



    /**
     * 登出方法（加入Token黑名单，确保登出后令牌失效）
     */
    @Override
    public Result<?> logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Result.error("Token不能为空");
        }

        try {
            if (!jwtTokenUtil.isTokenValid(token)) {
                return Result.error("无效的Token，无需登出");
            }

            // 1. 加入黑名单
            jwtTokenUtil.addTokenToBlacklist(token);
            // 2. 删除Redis用户信息
            String userInfoKey = jwtTokenUtil.buildUserInfoKey(token);
            redisUtil.delete(userInfoKey);

            return Result.success("退出登录成功");
        } catch (Exception e) {
            logger.error("登出过程发生异常", e);
            return Result.error("退出登录失败，请联系管理员");
        }
    }

    /**
     * 将Token加入黑名单
     */
    private void addTokenToBlacklist(String token) {
        // 计算Token剩余有效时间
        long expirationTime = jwtTokenUtil.getExpirationFromToken(token).getTime() - System.currentTimeMillis();
        if (expirationTime > 0) {
            // 将Token存入Redis，设置过期时间为原Token的剩余有效时间
            String blacklistKey = buildBlacklistKey(token);
            redisUtil.set(blacklistKey, "invalid", expirationTime, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isTokenInBlacklist(String token) {
        String blacklistKey = buildBlacklistKey(token);
        return redisUtil.hasKey(blacklistKey);
    }

    /**
     * 手动解锁用户（由具有用户管理编辑权限的本机构管理员调用）
     * @param userId 用户ID
     * @param orgId 机构ID
     * @return 解锁结果
     */
    public Result<?> unlockUser(Long userId, Long orgId) {
        try {
            // 验证orgId参数
            if (orgId == null) {
                return Result.error("机构ID不能为空");
            }

            // 根据用户ID查询用户信息
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 验证用户是否属于指定机构
            if (!orgId.equals(Long.valueOf(user.getOrgId()))) {
                return Result.error("无权限操作其他机构用户");
            }

            // 验证用户不是超级管理员
            if (user.getIsSysAdmin() != null && user.getIsSysAdmin() == 1) {
                return Result.error("超级管理员不能被锁定或解锁");
            }

            // 清除锁定状态
            String lockKey = buildLockKey(user.getAccount(), orgId.toString());
            if (redisUtil.hasKey(lockKey)) {
                redisUtil.delete(lockKey);
            }

            // 清除错误计数 - 使用统一的键名构建方法
            String uniqueUsername = user.getAccount() + "@" + orgId;
            String errorCountKey = buildErrorCountKey(uniqueUsername);
            String errorTimeKey = buildErrorTimeKey(uniqueUsername);
            redisUtil.delete(errorCountKey);
            redisUtil.delete(errorTimeKey);

            logger.info("用户解锁成功: {}@{}", user.getAccount(), orgId);
            return Result.success("用户解锁成功");
        } catch (Exception e) {
            logger.error("用户解锁失败", e);
            return Result.error("用户解锁失败，请联系管理员");
        }
    }

    /**
     * 检查用户是否被锁定
     * @param account 账户
     * @param orgId 机构ID
     * @return 是否被锁定
     */
    public boolean isUserLocked(String account, String orgId) {
        // 超级管理员不受锁定限制
        SysUser user = sysUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getAccount, account)
                .eq(SysUser::getOrgId, orgId)
        );

        if (user != null && user.getIsSysAdmin() != null && user.getIsSysAdmin() == 1) {
            return false; // 超级管理员永远不被锁定
        }

        String lockKey = buildLockKey(account, orgId);
        return redisUtil.hasKey(lockKey);
    }
}
