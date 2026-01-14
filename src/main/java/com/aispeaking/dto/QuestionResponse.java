package com.aispeaking.dto;

import com.aispeaking.entity.Question;
import com.aispeaking.entity.enums.QuestionLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Question response
 * Basic information without lazy-loaded relations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    
    private Long id;
    private String content;
    private QuestionLevel level;
    private String category;
    private Long createdBy;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Convert Question entity to QuestionResponse DTO
     */
    public static QuestionResponse from(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .level(question.getLevel())
                .category(question.getCategory())
                .createdBy(question.getCreatedBy() != null ? question.getCreatedBy().getId() : null)
                .createdByUsername(question.getCreatedBy() != null ? question.getCreatedBy().getUsername() : null)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
