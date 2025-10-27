package com.example.iotserver.config;

import io.swagger.v3.oas.models.Components; // THÊM IMPORT
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement; // THÊM IMPORT
import io.swagger.v3.oas.models.security.SecurityScheme; // THÊM IMPORT
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth"; // Tên của scheme bảo mật

        return new OpenAPI()
                // THÊM CÁC DÒNG CẤU HÌNH BẢO MẬT
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")))
                // GIỮ NGUYÊN PHẦN INFO
                .info(new Info()
                        .title("Smart Farm IoT API")
                        .version("1.0.0")
                        .description(
                                "Tài liệu API cho hệ thống Giám sát và Tự động hóa Nông nghiệp Thông minh - Báo cáo Đồ án Tốt nghiệp.")
                        .termsOfService("http://swagger.io/terms/")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}