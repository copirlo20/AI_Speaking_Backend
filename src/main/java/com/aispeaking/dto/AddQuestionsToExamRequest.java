package com.aispeaking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * DTO for adding questions to an exam
 */
@Data
public class AddQuestionsToExamRequest {
    
    @NotNull(message = "Question IDs are required")
    @NotEmpty(message = "At least one question ID is required")
    private List<Long> questionIds;
}
