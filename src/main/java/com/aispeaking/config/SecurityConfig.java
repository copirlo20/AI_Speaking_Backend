package com.aispeaking.config;

import com.aispeaking.security.CustomUserDetailsService;
import com.aispeaking.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @SuppressWarnings("deprecation")
    @Bean
    DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setUserDetailsService(customUserDetailsService);
        return authProvider;
    }
    
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // ============================================
                // CÁC ENDPOINT CÔNG KHAI (Không cần xác thực)
                // ============================================
                // Các endpoint xác thực
                .requestMatchers("/auth/login", "/auth/register", "/auth/check-username/**").permitAll()
                // Phiên kiểm tra - Hành động của sinh viên (không cần đăng nhập)
                .requestMatchers(HttpMethod.POST, "/test-sessions").permitAll() // Sinh viên bắt đầu kiểm tra
                .requestMatchers(HttpMethod.GET, "/test-sessions/{id}").permitAll() // Sinh viên xem phiên của họ
                .requestMatchers(HttpMethod.GET, "/test-sessions/{id}/answers").permitAll() // Sinh viên xem câu trả lời của họ
                .requestMatchers(HttpMethod.POST, "/test-sessions/{id}/submit-answer").permitAll() // Sinh viên nộp câu trả lời
                .requestMatchers(HttpMethod.POST, "/test-sessions/{id}/complete").permitAll() // Sinh viên hoàn thành kiểm tra
                // ============================================
                // Quản lý Hệ thống - chỉ ADMIN
                // ============================================
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Quản lý Người dùng - chỉ ADMIN
                .requestMatchers("/users/**").hasRole("ADMIN")
                // ============================================
                // TEACHER + ADMIN - Quản lý Ngân hàng Câu hỏi
                // ============================================
                .requestMatchers(HttpMethod.GET, "/questions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/questions/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/questions/search").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/questions/random").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/questions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/questions/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/questions/{id}").hasAnyRole("TEACHER", "ADMIN")
                // Quản lý Câu trả lời Mẫu (lồng trong questions)
                .requestMatchers(HttpMethod.GET, "/questions/{questionId}/sample-answers").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/questions/{questionId}/sample-answers/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/questions/{questionId}/sample-answers").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/questions/{questionId}/sample-answers/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/questions/{questionId}/sample-answers/{id}").hasAnyRole("TEACHER", "ADMIN")
                // ============================================
                // TEACHER + ADMIN - Quản lý Đề thi
                // ============================================
                .requestMatchers(HttpMethod.GET, "/exams").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/exams/search").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/exams/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/exams/{id}/questions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/exams").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/exams/{id}/questions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/exams/{id}/generate-random").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/exams/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/exams/{id}").hasAnyRole("TEACHER", "ADMIN")
                // ============================================
                // TEACHER + ADMIN - Xem và Quản lý Phiên kiểm tra
                // ============================================
                .requestMatchers(HttpMethod.GET, "/test-sessions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/test-sessions/search").hasAnyRole("TEACHER", "ADMIN")
                // Lưu ý: GET /test-sessions/{id} và /test-sessions/{id}/answers là công khai (ở trên)
                // ============================================
                // TEACHER + ADMIN - Statistics & Reports
                // ============================================
                .requestMatchers("/statistics/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/reports/**").hasAnyRole("TEACHER", "ADMIN")
                // ============================================
                // Mặc định: Tất cả các yêu cầu khác yêu cầu xác thực
                // ============================================
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}