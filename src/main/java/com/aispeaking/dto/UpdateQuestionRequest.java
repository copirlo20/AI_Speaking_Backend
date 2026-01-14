package com.aispeaking.dto;

import com.aispeaking.entity.enums.QuestionLevel;
import lombok.Data;

/**
 * DTO for updating a question
 */
@Data
public class UpdateQuestionRequest {
    
    private String content;
    private QuestionLevel level;
    private String category;
}
