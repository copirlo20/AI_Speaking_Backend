package com.aispeaking.dto;

import com.aispeaking.entity.enums.QuestionLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for generating random exam questions
 */
@Data
public class GenerateRandomExamRequest {
    
    private QuestionLevel level;
    
    @NotNull(message = "Question count is required")
    @Min(value = 1, message = "Count must be at least 1")
    private Integer count;
}
