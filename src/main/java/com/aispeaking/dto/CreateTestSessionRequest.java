package com.aispeaking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a new test session
 */
@Data
public class CreateTestSessionRequest {
    
    @NotNull(message = "Exam ID is required")
    private Long examId;
    
    @NotBlank(message = "Student name is required")
    private String studentName;
    
    private String studentOrganization;
    
    @Email(message = "Invalid email format")
    private String studentEmail;
}
