package com.aispeaking.dto;

import com.aispeaking.entity.enums.ExamStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO cho việc tạo đề thi mới
 */
@Data
public class CreateExamRequest {
    private String description;
    private Integer durationMinutes;
    private ExamStatus status;
    
    @NotNull(message = "Total questions is required")
    private Integer totalQuestions;
    
    @NotBlank(message = "Exam name is required")
    private String name;
}