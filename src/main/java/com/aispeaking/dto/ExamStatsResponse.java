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
public class ExamStatsResponse {
    private Long examId;
    private String examName;
    private Integer totalQuestions;
    private Long totalAttempts;
    private Long completedAttempts;
    private BigDecimal averageScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private Long passedCount;
    private BigDecimal passRate;
}
