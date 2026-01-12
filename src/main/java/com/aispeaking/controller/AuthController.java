package com.aispeaking.controller;

import com.aispeaking.dto.LoginRequest;
import com.aispeaking.dto.RegisterRequest;
import com.aispeaking.dto.AuthResponse;
import com.aispeaking.model.User;
import com.aispeaking.security.JwtTokenProvider;
import com.aispeaking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());
            user.setFullName(registerRequest.getFullName());
            user.setRole(User.UserRole.STUDENT);

            User createdUser = userService.createUser(user);
            String token = tokenProvider.generateToken(createdUser.getUsername());

            return ResponseEntity.ok(new AuthResponse(token, createdUser.getId(), 
                createdUser.getUsername(), createdUser.getEmail(), createdUser.getRole().toString()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            String token = tokenProvider.generateToken(authentication.getName());
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(new AuthResponse(token, user.getId(), 
                user.getUsername(), user.getEmail(), user.getRole().toString()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid username or password");
        }
    }
}
