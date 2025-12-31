package com.train.filter;

import com.train.security.JwtUserDetails;
import com.train.util.JwtTokenUtil;
import com.train.util.RedisUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final String loginPath;

    public JwtAuthorizationFilter(JwtTokenUtil jwtTokenUtil, String loginPath ) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.loginPath = loginPath.startsWith("/") ? loginPath : "/" + loginPath;
    }

    public JwtAuthorizationFilter(JwtTokenUtil jwtTokenUtil ) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.loginPath = "/auth/login";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. 放行登录请求
        String requestUri = request.getRequestURI();
        if (this.loginPath.equals(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 获取Token并校验格式
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authorizationHeader.substring(7);

        // 3. 前置三重校验（核心：确保过期/无效Token直接拦截）
        // 3.1 校验Token是否有效（过期 + 非黑名单 + 能解析）
        if (!jwtTokenUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 3.2 校验Redis用户信息缓存是否存在（即使JWT未过期，缓存过期也判定无效）
        String userInfoKey = jwtTokenUtil.buildUserInfoKey(token);
        if (!jwtTokenUtil.hasKey(userInfoKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. 解析Token并封装用户信息（仅处理有效Token）
        Claims claims = jwtTokenUtil.getClaimsFromToken(token);
        if (claims == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        String account = claims.get("account", String.class);
        String userName = claims.get("userName", String.class);
        String orgId = claims.get("orgId", String.class);
        Integer isSysAdmin = claims.get("isSysAdmin", Integer.class);
        List<String> authorityStrList = claims.get("authorities", List.class);

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (authorityStrList != null && !authorityStrList.isEmpty()) {
            for (String authorityStr : authorityStrList) {
                authorities.add(new SimpleGrantedAuthority(authorityStr));
            }
        }

        JwtUserDetails userDetails = new JwtUserDetails();
        userDetails.setUserId(userId);
        userDetails.setAccount(account);
        userDetails.setUserName(userName);
        userDetails.setOrgId(orgId);
        userDetails.setIsSysAdmin(isSysAdmin);
        userDetails.setAuthorities(authorities);
        userDetails.setStatus(1);

        // 5. 最终校验并授权
        boolean tokenValid = jwtTokenUtil.validateToken(token, userDetails);
        if (tokenValid) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}