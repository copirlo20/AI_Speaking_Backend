package com.aispeaking.dto;

import com.aispeaking.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long id;
    private String username;
    private String fullName;
    private UserRole role;
    private Boolean isActive;
    private String message;
    
    public LoginResponse(Long id, String username, String fullName, UserRole role, Boolean isActive) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
        this.message = "Login successful";
    }
    
    public LoginResponse(Long id, String username, String fullName, UserRole role, Boolean isActive, String message) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
        this.message = message;
    }
}
