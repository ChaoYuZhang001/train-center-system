package com.train.security;

import com.alibaba.fastjson.JSON;
import com.train.dto.LoginDTO;
import com.train.util.JwtTokenUtil;
import com.train.vo.JwtTokenVO;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证过滤器：拦截登录请求，完成认证并生成JWT
 */
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final int MAX_REQUEST_SIZE = 1024 * 10; // 10KB限制

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil) {
        if (authenticationManager == null) {
            throw new IllegalArgumentException("AuthenticationManager cannot be null");
        }
        if (jwtTokenUtil == null) {
            throw new IllegalArgumentException("JwtTokenUtil cannot be null");
        }
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        // 自定义登录接口路径（默认是/login，可修改）
//        this.setFilterProcessesUrl("/train/auth/login");
    }


    /**
     * 处理登录请求（认证逻辑）
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            // 1. 验证请求内容 - 放宽Content-Type要求
            String contentType = request.getContentType();
            if (contentType != null && !contentType.isEmpty()) {
                // 如果提供了Content-Type，检查是否为JSON类型
                if (!contentType.toLowerCase().contains(JSON_CONTENT_TYPE)) {
                    throw new IllegalArgumentException("Content-Type must be application/json if provided");
                }
            }

            // 2. 读取请求体并验证 - 使用更安全的方式读取请求体
            String requestBody;
            try (BufferedReader reader = request.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                int totalLength = 0;

                while ((line = reader.readLine()) != null) {
                    totalLength += line.length();
                    if (totalLength > MAX_REQUEST_SIZE) {
                        throw new IllegalArgumentException("Request body too large");
                    }
                    sb.append(line);
                }
                requestBody = sb.toString().trim();
            } catch (IllegalStateException e) {
                // 如果请求体已经被其他过滤器读取，尝试从参数获取
                String account = request.getParameter("account");
                String password = request.getParameter("password");
                String orgId = request.getParameter("orgId");

                if (account == null || password == null) {
                    throw new IllegalArgumentException("Request body cannot be empty and parameters are missing");
                }

                String username = account + "@" + (orgId != null ? orgId : "");
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        password
                );

                return authenticationManager.authenticate(authToken);
            }

            if (requestBody == null || requestBody.isEmpty()) {
                // 如果请求体为空，尝试从请求参数获取（兼容表单提交）
                String account = request.getParameter("account");
                String password = request.getParameter("password");
                String orgId = request.getParameter("orgId");

                if (account != null && password != null) {
                    String username = account + "@" + (orgId != null ? orgId : "");
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            password
                    );

                    return authenticationManager.authenticate(authToken);
                }

                throw new IllegalArgumentException("Request body cannot be empty");
            }

            // 3. 解析JSON
            LoginDTO loginDTO = JSON.parseObject(requestBody, LoginDTO.class);
            if (loginDTO == null) {
                throw new IllegalArgumentException("Failed to parse login request body");
            }

            // 验证必要字段
            if (loginDTO.getAccount() == null || loginDTO.getPassword() == null) {
                throw new IllegalArgumentException("Account and password are required");
            }

            // 2. 构建认证令牌：用户名=账号@机构ID，密码=明文密码
            String username = loginDTO.getAccount() + "@" + (loginDTO.getOrgId() != null ? loginDTO.getOrgId() : "");
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username,
                    loginDTO.getPassword()
            );

            // 3. 调用认证管理器完成认证
            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read request body", e);
        }
    }




    /**
     * 认证成功后的处理（生成JWT并返回）
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        // 1. 获取自定义用户详情
        JwtUserDetails userDetails = (JwtUserDetails) authResult.getPrincipal();

        // 2. 生成JWT令牌
        String accessToken = jwtTokenUtil.generateToken(userDetails);

        // 3. 构建响应结果
        JwtTokenVO jwtTokenVO = new JwtTokenVO();
        jwtTokenVO.setAccessToken(accessToken);
        jwtTokenVO.setExpiresIn(jwtTokenUtil.getExpiration());
        jwtTokenVO.setUserInfo(userDetails);

        // 4. 返回JSON格式响应
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.toJSONString(jwtTokenVO));
    }

    /**
     * 认证失败后的处理（统一返回错误信息）
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        // 1. 构建错误响应
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("code", 401);
        errorMap.put("msg", "账号、密码或机构错误，认证失败");
        errorMap.put("data", null);

        // 2. 返回JSON格式响应
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(JSON.toJSONString(errorMap));
    }
}
