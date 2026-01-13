package com.aispeaking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Long totalQuestions;
    private Long totalExams;
    private Long activeExams;
    private Long totalTestSessions;
    private Long completedSessions;
    private Long activeUsers;
    private BigDecimal averageScore;
    
    private Map<String, Long> questionsByLevel;
    private Map<String, Long> examsByStatus;
    private Map<String, Long> sessionsByStatus;
}
