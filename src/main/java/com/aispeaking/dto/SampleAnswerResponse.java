package com.aispeaking.dto;

import com.aispeaking.entity.SampleAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for SampleAnswer response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleAnswerResponse {
    
    private Long id;
    private Long questionId;
    private String content;
    private BigDecimal score;
    private String explanation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Convert SampleAnswer entity to SampleAnswerResponse DTO
     */
    public static SampleAnswerResponse from(SampleAnswer sampleAnswer) {
        return SampleAnswerResponse.builder()
                .id(sampleAnswer.getId())
                .questionId(sampleAnswer.getQuestion() != null ? sampleAnswer.getQuestion().getId() : null)
                .content(sampleAnswer.getContent())
                .score(sampleAnswer.getScore())
                .explanation(sampleAnswer.getExplanation())
                .createdAt(sampleAnswer.getCreatedAt())
                .updatedAt(sampleAnswer.getUpdatedAt())
                .build();
    }
}
