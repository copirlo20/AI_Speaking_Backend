package com.aispeaking.dto;

import com.aispeaking.entity.enums.QuestionLevel;
import lombok.Data;

/**
 * DTO cho việc cập nhật câu hỏi
 */
@Data
public class UpdateQuestionRequest {
    private String content;
    private QuestionLevel level;
}