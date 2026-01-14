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
                // Public endpoints
                .requestMatchers("/auth/**", "/users/register").permitAll()
                
                // Question endpoints - TEACHER và ADMIN
                .requestMatchers(HttpMethod.GET, "/questions/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/questions/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/questions/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/questions/**").hasAnyRole("TEACHER", "ADMIN")
                
                // Exam endpoints - TEACHER và ADMIN
                .requestMatchers(HttpMethod.GET, "/exams/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/exams/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/exams/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/exams/**").hasAnyRole("TEACHER", "ADMIN")
                
                // Test Session endpoints - TEACHER (xem và chấm lại điểm)
                .requestMatchers(HttpMethod.GET, "/test-sessions/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/test-sessions/*/submit-answer").permitAll() // Học sinh submit
                .requestMatchers(HttpMethod.POST, "/test-sessions/*/complete").permitAll() // Học sinh complete
                .requestMatchers(HttpMethod.POST, "/test-sessions").permitAll() // Học sinh tạo session
                .requestMatchers(HttpMethod.PUT, "/test-sessions/**").hasAnyRole("TEACHER", "ADMIN") // Chấm lại
                .requestMatchers(HttpMethod.DELETE, "/test-sessions/**").hasRole("ADMIN")
                
                // Statistics endpoints - TEACHER và ADMIN (xem thống kê)
                .requestMatchers("/statistics/**").hasAnyRole("TEACHER", "ADMIN")
                
                // Report endpoints - TEACHER và ADMIN
                .requestMatchers("/reports/**").hasAnyRole("TEACHER", "ADMIN")
                
                // Admin endpoints - chỉ ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // User management - chỉ ADMIN
                .requestMatchers(HttpMethod.GET, "/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}