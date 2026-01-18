package com.aispeaking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO cho việc thay đổi mật khẩu người dùng
 */
@Data
public class ChangePasswordRequest {
    private String oldPassword; // Lựa chọn: để kiểm tra bảo mật bổ sung

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String newPassword;
}