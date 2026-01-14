package com.aispeaking.dto;

import com.aispeaking.entity.enums.QuestionLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for creating a new question
 */
@Data
public class CreateQuestionRequest {
    
    @NotBlank(message = "Question content is required")
    private String content;
    
    private QuestionLevel level;
    
    private String category;
}
