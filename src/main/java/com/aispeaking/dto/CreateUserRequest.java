package com.aispeaking.dto;

import lombok.Data;

/**
 * DTO cho việc tạo tài khoản người dùng mới
 * Vai trò luôn được đặt là TEACHER theo mặc định (không thể được chỉ định bởi client)
 */
@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String fullName;
}