package com.aispeaking.dto;

import com.aispeaking.entity.ExamQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ExamQuestion response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionResponse {
    
    private Long id;
    private Long examId;
    private Long questionId;
    private String questionContent;
    private Integer questionOrder;
    private LocalDateTime createdAt;
    
    /**
     * Convert ExamQuestion entity to ExamQuestionResponse DTO
     */
    public static ExamQuestionResponse from(ExamQuestion examQuestion) {
        return ExamQuestionResponse.builder()
                .id(examQuestion.getId())
                .examId(examQuestion.getExam() != null ? examQuestion.getExam().getId() : null)
                .questionId(examQuestion.getQuestion() != null ? examQuestion.getQuestion().getId() : null)
                .questionContent(examQuestion.getQuestion() != null ? examQuestion.getQuestion().getContent() : null)
                .questionOrder(examQuestion.getQuestionOrder())
                .createdAt(examQuestion.getCreatedAt())
                .build();
    }
}
