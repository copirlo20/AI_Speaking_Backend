package com.aispeaking.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for updating a sample answer
 */
@Data
public class UpdateSampleAnswerRequest {
    
    private String content;
    private BigDecimal score;
}
