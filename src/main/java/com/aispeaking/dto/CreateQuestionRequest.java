package com.aispeaking.dto;

import com.aispeaking.entity.enums.QuestionLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for creating a new question
 */
@Data
public class CreateQuestionRequest {
    
    @NotBlank(message = "Question content is required")
    private String content;
    
    private QuestionLevel level;
    
    @Valid
    private List<SampleAnswerDto> sampleAnswers;
    
    @Data
    public static class SampleAnswerDto {
        @NotBlank(message = "Sample answer content is required")
        private String content;
        
        private BigDecimal score;
    }
}
