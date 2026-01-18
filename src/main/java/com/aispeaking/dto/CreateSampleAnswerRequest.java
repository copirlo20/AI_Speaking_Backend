package com.aispeaking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO cho việc tạo câu trả lời mẫu mới
 */
@Data
public class CreateSampleAnswerRequest {
    @NotBlank(message = "Sample answer content is required")
    private String content;
    
    @NotNull(message = "Score is required")
    private BigDecimal score;
}