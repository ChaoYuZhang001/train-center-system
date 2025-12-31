package com.train.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger3 配置类
 * 访问地址：http://127.0.0.1:8888/swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置OpenAPI文档基本信息
     */
    @Bean
    public OpenAPI trainCenterOpenAPI() {
        // 1. 配置JWT安全方案（添加Token请求头）
        SecurityScheme jwtSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // 协议类型
                .scheme("bearer") // 认证方案
                .bearerFormat("JWT") // Token格式
                .name("Authorization") // 请求头名称
                .in(SecurityScheme.In.HEADER); // 存放位置（请求头）

        // 2. 全局添加JWT安全验证
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("JWT");

        // 3. 构建OpenAPI文档信息
        return new OpenAPI()
                .info(new Info()
                        .title("培训中心系统接口文档") // 文档标题
                        .description("包含系统管理、培训视频、在线练习、质量评价等模块接口") // 文档描述
                        .version("1.0.0") // 版本号
                        .contact(new Contact() // 联系人信息
                                .name("开发团队")
                                .email("train@example.com"))
                        .license(new License() // 许可证信息
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components().addSecuritySchemes("JWT", jwtSecurityScheme)) // 注册JWT安全方案
                .addSecurityItem(securityRequirement); // 全局启用JWT认证
    }
}