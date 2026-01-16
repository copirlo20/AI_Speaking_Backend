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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @SuppressWarnings("deprecation")
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setUserDetailsService(customUserDetailsService);
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // ============================================
                // PUBLIC ENDPOINTS (No authentication required)
                // ============================================
                // Auth endpoints
                .requestMatchers("/auth/login", "/auth/register", "/auth/check-username/**").permitAll()
                
                // Test Session - Student actions (no login required)
                .requestMatchers(HttpMethod.POST, "/test-sessions").permitAll() // Student starts test
                .requestMatchers(HttpMethod.GET, "/test-sessions/{id}").permitAll() // Student views their session
                .requestMatchers(HttpMethod.GET, "/test-sessions/{id}/answers").permitAll() // Student views their answers
                .requestMatchers(HttpMethod.POST, "/test-sessions/{id}/submit-answer").permitAll() // Student submits answer
                .requestMatchers(HttpMethod.POST, "/test-sessions/{id}/complete").permitAll() // Student completes test
                
                // ============================================
                // ADMIN ONLY - Full access
                // ============================================
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // User Management - ADMIN only
                .requestMatchers("/users/**").hasRole("ADMIN")
                
                // ============================================
                // TEACHER + ADMIN - Question Bank Management
                // ============================================
                .requestMatchers(HttpMethod.GET, "/questions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/questions/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/questions/search").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/questions/random").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/questions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/questions/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/questions/{id}").hasAnyRole("TEACHER", "ADMIN")
                
                // Sample Answers Management (nested under questions)
                .requestMatchers(HttpMethod.GET, "/questions/{questionId}/sample-answers").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/questions/{questionId}/sample-answers/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/questions/{questionId}/sample-answers").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/questions/{questionId}/sample-answers/{id}").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/questions/{questionId}/sample-answers/{id}").hasAnyRole("TEACHER", "ADMIN")
                
                // ============================================
                // TEACHER + ADMIN - Exam Management
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
                // TEACHER + ADMIN - Test Session Viewing & Management
                // ============================================
                .requestMatchers(HttpMethod.GET, "/test-sessions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/test-sessions/search").hasAnyRole("TEACHER", "ADMIN")
                // Note: GET /test-sessions/{id} and /test-sessions/{id}/answers are public (above)
                
                // ============================================
                // TEACHER + ADMIN - Statistics & Reports
                // ============================================
                .requestMatchers("/statistics/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/reports/**").hasAnyRole("TEACHER", "ADMIN")
                
                // ============================================
                // Default: All other requests require authentication
                // ============================================
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}