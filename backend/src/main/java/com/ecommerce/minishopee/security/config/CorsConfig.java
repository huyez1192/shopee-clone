package com.ecommerce.minishopee.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origin của frontend React/Vite
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));

        // Các HTTP method frontend được phép gọi
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        // Các header frontend được phép gửi
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        // JWT hiện được gửi bằng Authorization header, chưa dùng cookie nên có thể để false.
        configuration.setAllowCredentials(false);

        // Cache kết quả preflight trong 1 giờ
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}