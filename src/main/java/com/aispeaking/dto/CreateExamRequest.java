package com.aispeaking.dto;

import com.aispeaking.entity.enums.ExamStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a new exam
 */
@Data
public class CreateExamRequest {
    
    @NotBlank(message = "Exam name is required")
    private String name;
    
    private String description;
    
    private Integer durationMinutes;
    
    @NotNull(message = "Total questions is required")
    private Integer totalQuestions;
    
    private ExamStatus status;
}
