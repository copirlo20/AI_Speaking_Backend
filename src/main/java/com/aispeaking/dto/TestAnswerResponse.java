package com.aispeaking.dto;

import com.aispeaking.entity.TestAnswer;
import com.aispeaking.entity.enums.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho phản hồi TestAnswer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestAnswerResponse {
    private Long id;
    private Long testSessionId;
    private Long questionId;
    private String questionContent;
    private String audioUrl;
    private String transcribedText;
    private BigDecimal score;
    private String feedback;
    private ProcessingStatus processingStatus;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;
    
    /**
     * Chuyển đổi entity TestAnswer thành DTO TestAnswerResponse
     */
    public static TestAnswerResponse from(TestAnswer testAnswer) {
        return TestAnswerResponse.builder()
                .id(testAnswer.getId())
                .testSessionId(testAnswer.getTestSession() != null ? testAnswer.getTestSession().getId() : null)
                .questionId(testAnswer.getQuestion() != null ? testAnswer.getQuestion().getId() : null)
                .questionContent(testAnswer.getQuestion() != null ? testAnswer.getQuestion().getContent() : null)
                .audioUrl(testAnswer.getAudioUrl())
                .transcribedText(testAnswer.getTranscribedText())
                .score(testAnswer.getScore())
                .feedback(testAnswer.getFeedback())
                .processingStatus(testAnswer.getProcessingStatus())
                .answeredAt(testAnswer.getAnsweredAt())
                .createdAt(testAnswer.getCreatedAt())
                .build();
    }
}