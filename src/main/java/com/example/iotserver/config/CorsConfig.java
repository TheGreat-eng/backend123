package com.example.iotserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
// public class CorsConfig {

// @Bean
// public CorsFilter corsFilter() {
// CorsConfiguration config = new CorsConfiguration();
// config.setAllowCredentials(true);
// config.setAllowedOrigins(Arrays.asList("http://localhost:3000",
// "http://localhost:3001"));
// config.setAllowedHeaders(Arrays.asList("*"));
// config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE",
// "OPTIONS"));

// UrlBasedCorsConfigurationSource source = new
// UrlBasedCorsConfigurationSource();
// source.registerCorsConfiguration("/**", config);

// return new CorsFilter(source);
// }
// }
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow all origins for development
        config.addAllowedOriginPattern("*");

        // Allow credentials
        config.setAllowCredentials(true);

        // Allow all headers
        config.addAllowedHeader("*");

        // Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Expose headers
        config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}