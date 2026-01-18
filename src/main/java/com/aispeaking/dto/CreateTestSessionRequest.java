package com.aispeaking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO cho việc tạo phiên kiểm tra mới
 */
@Data
public class CreateTestSessionRequest {
    private String studentOrganization;
    
    @NotNull(message = "Exam ID is required")
    private Long examId;
    
    @NotBlank(message = "Student name is required")
    private String studentName;
}