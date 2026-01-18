package com.aispeaking.dto;

import com.aispeaking.entity.enums.UserRole;
import lombok.Data;

/**
 * DTO cho cập nhật thông tin người dùng
 * Không bao gồm mật khẩu (sử dụng ChangePasswordRequest cho việc đó)
 */
@Data
public class UpdateUserRequest {
    private String fullName;
    private UserRole role;
    private Boolean isActive;
}