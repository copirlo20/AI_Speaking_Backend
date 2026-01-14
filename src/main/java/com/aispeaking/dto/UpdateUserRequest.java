package com.aispeaking.dto;

import com.aispeaking.entity.enums.UserRole;
import lombok.Data;

/**
 * DTO for updating user information
 * Does not include password (use ChangePasswordRequest for that)
 */
@Data
public class UpdateUserRequest {
    
    private String fullName;
    private UserRole role;
    private Boolean isActive;
}
