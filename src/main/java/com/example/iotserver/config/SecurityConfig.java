package com.example.iotserver.config;

import com.example.iotserver.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // Disable CSRF for REST API
                                .csrf(csrf -> csrf.disable())

                                // Configure authorization
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints - không cần authentication
                                                .requestMatchers(
                                                                "/actuator/**",
                                                                "/api/auth/**",

                                                                // VVVV--- SỬA LẠI ĐƯỜNG DẪN Ở ĐÂY ---VVVV
                                                                // SockJS sẽ gọi đến /ws và /ws/info...
                                                                // Axios instance có baseURL là /api, nên đường dẫn thực
                                                                // tế là /api/ws/**
                                                                // Tuy nhiên, endpoint /ws của Spring không nằm dưới
                                                                // /api.
                                                                // Vấn đề là frontend đang gọi sai. Chúng ta cần sửa cả
                                                                // frontend và backend.

                                                                // CÁCH SỬA ĐÚNG:
                                                                // 1. Cho phép /ws/** truy cập công khai
                                                                "/ws/**",

                                                                // ^^^^----------------------------------^^^^

                                                                "/error",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                // Tất cả endpoints khác cần authentication
                                                .anyRequest().authenticated())

                                // Stateless session (for REST API)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Add JWT filter
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }
}