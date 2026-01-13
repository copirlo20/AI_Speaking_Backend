package com.aispeaking.controller;

import com.aispeaking.dto.CreateUserRequest;
import com.aispeaking.dto.LoginRequest;
import com.aispeaking.dto.LoginResponse;
import com.aispeaking.entity.User;
import com.aispeaking.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
@Slf4j
public class AuthController {

    private final UserService userService;

    /**
     * Login endpoint
     * POST /api/auth/login
     * 
     * Request body:
     * {
     *   "username": "admin",
     *   "password": "admin123"
     * }
     * 
     * Response:
     * {
     *   "id": 1,
     *   "username": "admin",
     *   "fullName": "Admin User",
     *   "role": "ADMIN",
     *   "isActive": true,
     *   "message": "Login successful"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            
            LoginResponse response = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getIsActive()
            );
            
            log.info("User {} logged in successfully", request.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Login failed for username: {}", request.getUsername());
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * Register new TEACHER account
     * POST /api/auth/register
     * 
     * Request body:
     * {
     *   "username": "teacher01",
     *   "password": "password123",
     *   "fullName": "Nguyen Van A"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CreateUserRequest request) {
        try {
            User user = userService.createTeacherAccount(
                request.getUsername(),
                request.getPassword(),
                request.getFullName()
            );
            
            LoginResponse response = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getIsActive(),
                "Registration successful"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Check if username exists
     * GET /api/auth/check-username/{username}
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@PathVariable String username) {
        boolean exists = userService.usernameExists(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}
