package com.aispeaking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestSubmissionRequest {
    private Long userId;
    private Long testId;
    private Long questionId;
}
