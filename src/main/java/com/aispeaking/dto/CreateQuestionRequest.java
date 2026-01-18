package com.aispeaking.dto;

import com.aispeaking.entity.enums.QuestionLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO cho việc tạo câu hỏi mới
 */
@Data
public class CreateQuestionRequest {
    private QuestionLevel level;
    
    @NotBlank(message = "Question content is required")
    private String content;
    
    @Valid
    private List<SampleAnswerDto> sampleAnswers;
    
    @Data
    public static class SampleAnswerDto {
        private BigDecimal score;

        @NotBlank(message = "Sample answer content is required")
        private String content;
    }
}