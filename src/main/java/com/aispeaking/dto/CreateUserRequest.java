package com.aispeaking.dto;

import lombok.Data;

/**
 * DTO for creating new user account
 * Role is always set to TEACHER by default (cannot be specified by client)
 */
@Data
public class CreateUserRequest {
    
    private String username;
    
    private String password;
    
    private String fullName;
}
