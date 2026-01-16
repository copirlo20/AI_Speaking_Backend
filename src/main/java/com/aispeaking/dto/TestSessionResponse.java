package com.aispeaking.dto;

import com.aispeaking.entity.TestSession;
import com.aispeaking.entity.enums.TestSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for TestSession response
 * Basic information without lazy-loaded relations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSessionResponse {
    
    private Long id;
    private Long examId;
    private String examName;
    private String studentName;
    private String studentOrganization;
    private BigDecimal totalScore;
    private TestSessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    
    /**
     * Convert TestSession entity to TestSessionResponse DTO
     */
    public static TestSessionResponse from(TestSession testSession) {
        return TestSessionResponse.builder()
                .id(testSession.getId())
                .examId(testSession.getExam() != null ? testSession.getExam().getId() : null)
                .examName(testSession.getExam() != null ? testSession.getExam().getName() : null)
                .studentName(testSession.getStudentName())
                .studentOrganization(testSession.getStudentOrganization())
                .totalScore(testSession.getTotalScore())
                .status(testSession.getStatus())
                .startedAt(testSession.getStartedAt())
                .completedAt(testSession.getCompletedAt())
                .createdAt(testSession.getCreatedAt())
                .build();
    }
}
