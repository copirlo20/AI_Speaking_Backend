package com.aispeaking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI scoring response with detailed breakdown
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIScoreResponse {
    
    private Double pronunciationScore;
    private Double fluencyScore;
    private Double grammarScore;
    private Double vocabularyScore;
    private Double contentScore;
    private Double overallScore;
    
    private String feedback;
    private List<String> suggestions;
    
    // Detailed breakdown
    private PronunciationDetails pronunciationDetails;
    private FluencyDetails fluencyDetails;
    private GrammarDetails grammarDetails;
    private VocabularyDetails vocabularyDetails;
    private ContentDetails contentDetails;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PronunciationDetails {
        private Integer wordCount;
        private Integer mispronunciationCount;
        private List<String> problematicWords;
        private Double clarity;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FluencyDetails {
        private Double wordsPerMinute;
        private Integer pauseCount;
        private Integer fillerWordCount;
        private List<String> fillerWords;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrammarDetails {
        private Integer totalSentences;
        private Integer grammaticalErrors;
        private List<String> errorTypes;
        private Double accuracy;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VocabularyDetails {
        private Integer uniqueWords;
        private Integer totalWords;
        private Double lexicalDiversity;
        private List<String> advancedWords;
        private String vocabularyLevel;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentDetails {
        private Double relevanceScore;
        private Double completenessScore;
        private Integer keyPointsCovered;
        private Integer totalKeyPoints;
        private List<String> missingPoints;
    }
}
