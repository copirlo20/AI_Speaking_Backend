package com.aispeaking.dto;

import com.aispeaking.entity.Exam;
import com.aispeaking.entity.enums.ExamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Exam response
 * Basic information without lazy-loaded relations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {
    
    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private ExamStatus status;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Convert Exam entity to ExamResponse DTO
     */
    public static ExamResponse from(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .description(exam.getDescription())
                .durationMinutes(exam.getDurationMinutes())
                .totalQuestions(exam.getTotalQuestions())
                .status(exam.getStatus())
                .createdByUsername(exam.getCreatedBy() != null ? exam.getCreatedBy().getUsername() : null)
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }
}
