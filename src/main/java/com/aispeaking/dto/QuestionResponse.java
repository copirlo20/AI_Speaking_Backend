package com.aispeaking.dto;

import com.aispeaking.entity.Question;
import com.aispeaking.entity.enums.QuestionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO cho phản hồi Question
 * Thông tin cơ bản không có các quan hệ lazy-loaded
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String content;
    private QuestionLevel level;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Chuyển đổi entity Question thành DTO QuestionResponse
     */
    public static QuestionResponse from(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .level(question.getLevel())
                .createdByUsername(question.getCreatedBy() != null ? question.getCreatedBy().getUsername() : null)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
