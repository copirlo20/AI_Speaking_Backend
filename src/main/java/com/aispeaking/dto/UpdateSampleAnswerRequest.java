package com.aispeaking.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO cho việc cập nhật câu trả lời mẫu
 */
@Data
public class UpdateSampleAnswerRequest {
    private String content;
    private BigDecimal score;
}