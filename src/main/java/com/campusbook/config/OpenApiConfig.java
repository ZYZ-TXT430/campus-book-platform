package com.campusbook.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置。
 *
 * <p>启动后访问：</p>
 * <ul>
 *   <li>Swagger UI：http://localhost:8080/swagger-ui/index.html</li>
 *   <li>OpenAPI JSON：http://localhost:8080/v3/api-docs</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI campusBookOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园二手书交易平台 API")
                        .description("用户、书籍、订单、举报、评价相关接口。所有接口统一返回 { code, message, data }。")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("先调用 /api/auth/login 获取 token，再填 Bearer <token>")));
    }
}
