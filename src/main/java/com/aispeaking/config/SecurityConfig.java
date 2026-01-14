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
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
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
                .requestMatchers("/auth/login", "/auth/register", "/auth/check-username/**").permitAll()
                
                // Test Session - Student actions (no login required)
                .requestMatchers(HttpMethod.POST, "/test-sessions").permitAll() // Student starts test
                .requestMatchers(HttpMethod.POST, "/test-sessions/*/answers").permitAll() // Student submits answer
                .requestMatchers(HttpMethod.POST, "/test-sessions/*/complete").permitAll() // Student completes test
                
                // ============================================
                // ADMIN ONLY - Full access
                // ============================================
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // User Management - ADMIN only
                .requestMatchers("/users/**").hasRole("ADMIN")
                
                // ============================================
                // TEACHER + ADMIN - Question Bank Management
                // ============================================
                .requestMatchers(HttpMethod.GET, "/questions/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/questions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/questions/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/questions/**").hasAnyRole("TEACHER", "ADMIN")
                
                // ============================================
                // TEACHER + ADMIN - Exam Management
                // ============================================
                .requestMatchers(HttpMethod.GET, "/exams/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/exams").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/exams/*/questions").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/exams/generate-random").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/exams/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/exams/**").hasAnyRole("TEACHER", "ADMIN")
                
                // ============================================
                // TEACHER + ADMIN - Test Session Viewing & Grading
                // ============================================
                .requestMatchers(HttpMethod.GET, "/test-sessions/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/test-sessions/**").hasAnyRole("TEACHER", "ADMIN") // Re-grade if needed
                
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