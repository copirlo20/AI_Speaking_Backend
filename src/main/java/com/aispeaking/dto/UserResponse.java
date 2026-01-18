package com.aispeaking.dto;

import com.aispeaking.entity.User;
import com.aispeaking.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO cho phản hồi User - KHÔNG BAO GIỞ lộ trường mật khẩu
 * Sử dụng DTO này thay vì entity User trong tất cả các phản hồi từ controller
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Chuyển đổi entity User thành DTO UserResponse
     * param user Entity User
     * return DTO UserResponse không có trường mật khẩu
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}