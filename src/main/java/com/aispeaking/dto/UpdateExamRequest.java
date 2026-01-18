package com.aispeaking.dto;

import com.aispeaking.entity.enums.ExamStatus;
import lombok.Data;

/**
 * DTO cho việc cập nhật đề thi
 */
@Data
public class UpdateExamRequest {
    private String name;
    private String description;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private ExamStatus status;
}