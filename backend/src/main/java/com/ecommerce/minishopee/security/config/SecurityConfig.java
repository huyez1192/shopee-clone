package com.ecommerce.minishopee.security.config;

import com.ecommerce.minishopee.security.jwt.JwtAuthenticationFilter;
import com.ecommerce.minishopee.security.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider)
        throws Exception {
            return http
                    .cors(Customizer.withDefaults())
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authenticationProvider(authenticationProvider)
                    .authorizeHttpRequests(auth -> auth

                            // =====================================================
                            // 1. PUBLIC API — KHÔNG CẦN ĐĂNG NHẬP
                            // =====================================================

                            .requestMatchers("/error").permitAll()

                            // Đăng ký, đăng nhập
                            .requestMatchers("/api/auth/**").permitAll()

                            // Xem sản phẩm
                            .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                            // Xem danh mục
                            .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()


                            // =====================================================
                            // 2. ADMIN API
                            // =====================================================

                            // Admin tạo, cập nhật, xóa sản phẩm
                            .requestMatchers(HttpMethod.POST, "/api/products/**")
                            .hasRole("ADMIN")

                            .requestMatchers(HttpMethod.PUT, "/api/products/**")
                            .hasRole("ADMIN")

                            .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                            .hasRole("ADMIN")

                            // Admin tạo, cập nhật, xóa danh mục
                            .requestMatchers(HttpMethod.POST, "/api/categories/**")
                            .hasRole("ADMIN")

                            .requestMatchers(HttpMethod.PUT, "/api/categories/**")
                            .hasRole("ADMIN")

                            .requestMatchers(HttpMethod.PATCH, "/api/categories/**")
                            .hasRole("ADMIN")

                            .requestMatchers(HttpMethod.DELETE, "/api/categories/**")
                            .hasRole("ADMIN")

                            // Toàn bộ API quản trị
                            .requestMatchers("/api/admin/**")
                            .hasRole("ADMIN")


                            // =====================================================
                            // 3. ĐĂNG KÝ VÀ QUẢN LÝ SHOP
                            // =====================================================

                            // User đăng nhập là được gửi yêu cầu đăng ký shop
                            .requestMatchers(HttpMethod.POST, "/api/shops/register")
                            .authenticated()

                            // User được xem và cập nhật yêu cầu shop của chính mình,
                            // kể cả khi shop đang ở trạng thái PENDING
                            .requestMatchers(HttpMethod.GET, "/api/seller/shop")
                            .authenticated()

                            .requestMatchers(HttpMethod.PUT, "/api/seller/shop")
                            .authenticated()


                            // =====================================================
                            // 4. SELLER API
                            // =====================================================

                            // Các API bán hàng thực sự chỉ dành cho SELLER
                            .requestMatchers("/api/seller/products/**")
                            .hasRole("SELLER")

                            .requestMatchers("/api/seller/orders/**")
                            .hasRole("SELLER")

                            // Các API seller còn lại
                            .requestMatchers("/api/seller/**")
                            .hasRole("SELLER")


                            // =====================================================
                            // 5. API CHO MỌI TÀI KHOẢN ĐÃ ĐĂNG NHẬP
                            // =====================================================

                            // USER, SELLER và ADMIN đều có thể mua hàng
                            .requestMatchers("/api/cart/**")
                            .authenticated()

                            .requestMatchers("/api/checkout/**")
                            .authenticated()

                            .requestMatchers("/api/orders/**")
                            .authenticated()

                            .requestMatchers("/api/users/**")
                            .authenticated()

                            .requestMatchers("/api/addresses/**")
                            .authenticated()


                            // =====================================================
                            // 6. MẶC ĐỊNH
                            // =====================================================

                            // Các API còn lại đều phải đăng nhập
                            .anyRequest()
                            .authenticated()
                    )

                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint((request, response, authException) -> {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.setCharacterEncoding("UTF-8");

                                response.getWriter().write("""
                                {
                                  "status": 401,
                                  "error": "Unauthorized",
                                  "message": "Authentication is required"
                                }
                                """);
                            })

                            .accessDeniedHandler((request, response, accessDeniedException) -> {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.setCharacterEncoding("UTF-8");

                                response.getWriter().write("""
                                {
                                  "status": 403,
                                  "error": "Forbidden",
                                  "message": "You do not have permission to access this resource"
                                }
                                """);
                            })
                    )

                    .addFilterBefore(
                            jwtAuthenticationFilter,
                            UsernamePasswordAuthenticationFilter.class
                    )

                    .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
        throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
