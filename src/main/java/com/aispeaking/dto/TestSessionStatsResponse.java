package com.aispeaking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSessionStatsResponse {
    private Integer totalQuestions;
    private Long answeredQuestions;
    private Long pendingQuestions;
    private Long failedQuestions;
    private BigDecimal averageScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
}
