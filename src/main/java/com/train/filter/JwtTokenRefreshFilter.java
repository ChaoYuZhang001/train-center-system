package com.train.filter;

import com.train.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT Token无感续期过滤器
 * 核心：接口请求成功（认证通过）后，自动更新Redis用户信息缓存过期时间，实现无感续期
 */
public class JwtTokenRefreshFilter extends OncePerRequestFilter {
    // 日志记录
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenRefreshFilter.class);

    // JWT工具类依赖
    private final JwtTokenUtil jwtTokenUtil;
    // 登录接口路径（无需续期，直接放行）
    private final String loginPath;

    /**
     * 自定义构造方法（推荐）：指定JwtTokenUtil和登录路径
     * @param jwtTokenUtil JWT工具类
     * @param loginPath 登录接口路径（如 /auth/login）
     */
    public JwtTokenRefreshFilter(JwtTokenUtil jwtTokenUtil, String loginPath) {
        this.jwtTokenUtil = jwtTokenUtil;
        // 统一路径格式，避免首尾斜杠问题（如传入 auth/login 自动转为 /auth/login）
        this.loginPath = loginPath.startsWith("/") ? loginPath : "/" + loginPath;
    }

    /**
     * 默认构造方法：使用默认登录路径 /auth/login
     * @param jwtTokenUtil JWT工具类
     */
    public JwtTokenRefreshFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.loginPath = "/auth/login";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. 放行登录请求：登录时无有效Token，无需续期
        String requestUri = request.getRequestURI();
        // 若项目有上下文路径（如 /train），建议使用 request.getServletPath() 替代 requestUri（避免路径匹配失败）
        // String servletPath = request.getServletPath();
        if (this.loginPath.equals(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 先执行接口请求（核心：先处理业务逻辑，确保请求成功后再续期）
        // 只有接口请求正常完成（认证通过、业务无异常），才会执行后续续期逻辑
        filterChain.doFilter(request, response);

        // 3. 判断是否认证通过：SecurityContextHolder存在Authentication说明已授权成功
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            logger.debug("请求未认证通过，不执行Token续期");
            return;
        }

        // 4. 提取请求头中的JWT Token
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logger.debug("请求头中无有效Token，不执行Token续期");
            return;
        }
        // 去除Bearer前缀，获取纯Token
        String token = authorizationHeader.substring(7).trim();

        // 5. 核心：判断是否需要续期 + 执行续期操作
        try {
            // 5.1 判断Token是否需要续期（剩余时间 < 续期阈值）
//            if (jwtTokenUtil.needRefresh(token)) {
            // 5.2 执行续期：更新Redis用户信息缓存的过期时间（重置为JWT原始过期时间）
            boolean refreshSuccess = jwtTokenUtil.refreshUserInfoCache(token);
            if (refreshSuccess) {
                logger.info("Token无感续期成功，Token后缀：{}", token.substring(Math.max(0, token.length() - 10)));
            } else {
                logger.warn("Token无感续期失败（Redis用户信息缓存不存在），Token后缀：{}", token.substring(Math.max(0, token.length() - 10)));
            }
//            } else {
//                logger.debug("Token剩余时间充足，无需续期，Token后缀：{}", token.substring(Math.max(0, token.length() - 10)));
//            }
        } catch (Exception e) {
            logger.error("Token无感续期异常，Token后缀：{}", token.substring(Math.max(0, token.length() - 10)), e);
        }
    }
}