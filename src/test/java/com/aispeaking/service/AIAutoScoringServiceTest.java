package com.aispeaking.service;

import com.aispeaking.dto.AIScoreResponse;
import com.aispeaking.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AIAutoScoringServiceTest {

    @Autowired
    private AIAutoScoringService scoringService;

    private Question testQuestion;

    @BeforeEach
    void setUp() {
        testQuestion = new Question();
        testQuestion.setId(1L);
        testQuestion.setQuestionText("Describe your favorite place to visit");
        testQuestion.setSampleAnswer("My favorite place to visit is the beach because I love swimming and relaxing by the ocean. The sound of waves is very calming and peaceful.");
        testQuestion.setDifficulty(Question.DifficultyLevel.INTERMEDIATE);
        testQuestion.setType(Question.QuestionType.DESCRIPTION);
    }

    @Test
    void testAutoScoreResponse_GoodQualityResponse() {
        // Arrange
        String transcription = "My favorite place to visit is definitely the mountains. " +
                "I absolutely love hiking through beautiful trails and enjoying the fresh air. " +
                "The scenery is breathtaking and the peaceful atmosphere helps me relax and unwind. " +
                "I particularly enjoy visiting during autumn when the leaves change colors. " +
                "It's an amazing experience that I highly recommend to everyone.";
        Integer duration = 20; // 20 seconds

        // Act
        AIScoreResponse response = scoringService.autoScoreResponse(transcription, testQuestion, duration);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getOverallScore());
        assertTrue(response.getOverallScore() >= 0.0 && response.getOverallScore() <= 10.0);
        
        // Check all component scores
        assertNotNull(response.getPronunciationScore());
        assertNotNull(response.getFluencyScore());
        assertNotNull(response.getGrammarScore());
        assertNotNull(response.getVocabularyScore());
        assertNotNull(response.getContentScore());
        
        // Good response should have decent scores
        assertTrue(response.getOverallScore() >= 6.0, "Overall score should be at least 6.0 for good response");
        
        // Check details
        assertNotNull(response.getPronunciationDetails());
        assertNotNull(response.getFluencyDetails());
        assertNotNull(response.getGrammarDetails());
        assertNotNull(response.getVocabularyDetails());
        assertNotNull(response.getContentDetails());
        
        // Check feedback
        assertNotNull(response.getFeedback());
        assertFalse(response.getFeedback().isEmpty());
        
        // Check suggestions
        assertNotNull(response.getSuggestions());
        
        System.out.println("=== Good Quality Response Results ===");
        System.out.println("Overall Score: " + response.getOverallScore());
        System.out.println("Pronunciation: " + response.getPronunciationScore());
        System.out.println("Fluency: " + response.getFluencyScore());
        System.out.println("Grammar: " + response.getGrammarScore());
        System.out.println("Vocabulary: " + response.getVocabularyScore());
        System.out.println("Content: " + response.getContentScore());
        System.out.println("\nFeedback:\n" + response.getFeedback());
        System.out.println("\nSuggestions: " + response.getSuggestions());
    }

    @Test
    void testAutoScoreResponse_PoorQualityResponse() {
        // Arrange
        String transcription = "Um, I like beach. Beach is good. I go there. Nice.";
        Integer duration = 8; // 8 seconds - very short

        // Act
        AIScoreResponse response = scoringService.autoScoreResponse(transcription, testQuestion, duration);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getOverallScore());
        
        // Poor response should have lower scores
        assertTrue(response.getOverallScore() < 6.0, "Overall score should be less than 6.0 for poor response");
        
        // Should have suggestions for improvement
        assertNotNull(response.getSuggestions());
        assertFalse(response.getSuggestions().isEmpty());
        
        System.out.println("\n=== Poor Quality Response Results ===");
        System.out.println("Overall Score: " + response.getOverallScore());
        System.out.println("Pronunciation: " + response.getPronunciationScore());
        System.out.println("Fluency: " + response.getFluencyScore());
        System.out.println("Grammar: " + response.getGrammarScore());
        System.out.println("Vocabulary: " + response.getVocabularyScore());
        System.out.println("Content: " + response.getContentScore());
        System.out.println("\nFeedback:\n" + response.getFeedback());
        System.out.println("\nSuggestions: " + response.getSuggestions());
    }

    @Test
    void testAutoScoreResponse_ExcellentResponse() {
        // Arrange
        String transcription = "My favorite destination to visit is undoubtedly the magnificent Swiss Alps. " +
                "The breathtaking panoramic views of snow-capped mountains create an absolutely stunning landscape. " +
                "I particularly appreciate the opportunity to engage in various outdoor activities such as skiing, " +
                "hiking, and mountain climbing. The pristine natural environment offers a perfect escape from urban life, " +
                "allowing me to reconnect with nature and rejuvenate my spirit. The charming alpine villages scattered " +
                "throughout the region add cultural richness to the experience. I thoroughly recommend visiting during " +
                "winter season when the entire area transforms into a winter wonderland.";
        Integer duration = 35; // 35 seconds - good duration

        // Act
        AIScoreResponse response = scoringService.autoScoreResponse(transcription, testQuestion, duration);

        // Assert
        assertNotNull(response);
        
        // Excellent response should have good scores (adjusted expectations)
        assertTrue(response.getOverallScore() >= 6.5, 
            "Overall score should be at least 6.5 for excellent response, got: " + response.getOverallScore());
        
        // Check vocabulary details
        assertNotNull(response.getVocabularyDetails());
        assertTrue(response.getVocabularyDetails().getAdvancedWords().size() > 3, 
            "Should have multiple advanced words");
        
        // Check fluency
        assertNotNull(response.getFluencyDetails());
        assertTrue(response.getFluencyDetails().getWordsPerMinute() > 100, 
            "Should have good speaking pace");
        
        System.out.println("\n=== Excellent Response Results ===");
        System.out.println("Overall Score: " + response.getOverallScore());
        System.out.println("Pronunciation: " + response.getPronunciationScore());
        System.out.println("Fluency: " + response.getFluencyScore());
        System.out.println("Grammar: " + response.getGrammarScore());
        System.out.println("Vocabulary: " + response.getVocabularyScore());
        System.out.println("Content: " + response.getContentScore());
        System.out.println("\nVocabulary Level: " + response.getVocabularyDetails().getVocabularyLevel());
        System.out.println("Advanced Words: " + response.getVocabularyDetails().getAdvancedWords());
        System.out.println("Words per Minute: " + response.getFluencyDetails().getWordsPerMinute());
        System.out.println("\nFeedback:\n" + response.getFeedback());
    }

    @Test
    void testAutoScoreResponse_WithFillerWords() {
        // Arrange
        String transcription = "Um, well, you know, I like, um, going to the beach, like, " +
                "it's really nice, you know, and, um, I think it's, like, very relaxing and stuff.";
        Integer duration = 15;

        // Act
        AIScoreResponse response = scoringService.autoScoreResponse(transcription, testQuestion, duration);

        // Assert
        assertNotNull(response.getFluencyDetails());
        System.out.println("\n=== Filler Words Detection ===");
        System.out.println("Transcription: " + transcription);
        System.out.println("Filler Word Count: " + response.getFluencyDetails().getFillerWordCount());
        System.out.println("Filler Words Found: " + response.getFluencyDetails().getFillerWords());
        
        assertTrue(response.getFluencyDetails().getFillerWordCount() > 0, 
            "Should detect filler words. Found: " + response.getFluencyDetails().getFillerWordCount());
        
        // Fluency score should be affected by filler words
        assertTrue(response.getFluencyScore() < 8.5, 
            "Fluency score should be lower due to filler words, got: " + response.getFluencyScore());
        
        System.out.println("Fluency Score: " + response.getFluencyScore());
        System.out.println("\nSuggestions: " + response.getSuggestions());
    }

    @Test
    void testAutoScoreResponse_ShortResponse() {
        // Arrange
        String transcription = "Beach. Good.";
        Integer duration = 3;

        // Act
        AIScoreResponse response = scoringService.autoScoreResponse(transcription, testQuestion, duration);

        // Assert
        assertNotNull(response);
        
        // Very short response should have low content score
        assertTrue(response.getContentScore() < 5.0, 
            "Content score should be low for very short response");
        
        // Should suggest providing more details
        assertTrue(response.getSuggestions().stream()
            .anyMatch(s -> s.toLowerCase().contains("detail") || s.toLowerCase().contains("example")),
            "Should suggest providing more details");
        
        System.out.println("\n=== Short Response Results ===");
        System.out.println("Overall Score: " + response.getOverallScore());
        System.out.println("Content Score: " + response.getContentScore());
        System.out.println("\nSuggestions: " + response.getSuggestions());
    }

    @Test
    void testPronunciationScoring() {
        // Test with clean transcription
        String cleanTranscription = "The beautiful landscape provides excellent opportunities for relaxation.";
        Integer duration = 10;
        
        AIScoreResponse response1 = scoringService.autoScoreResponse(cleanTranscription, testQuestion, duration);
        
        // Test with problematic transcription (simulating poor pronunciation)
        String problematicTranscription = "Da byutfl landskap provid exlnt oportunitis fo relaxashun.";
        
        AIScoreResponse response2 = scoringService.autoScoreResponse(problematicTranscription, testQuestion, duration);
        
        // Clean transcription should have higher pronunciation score
        assertTrue(response1.getPronunciationScore() > response2.getPronunciationScore(),
            "Clean transcription should have higher pronunciation score");
        
        System.out.println("\n=== Pronunciation Comparison ===");
        System.out.println("Clean: " + response1.getPronunciationScore());
        System.out.println("Problematic: " + response2.getPronunciationScore());
    }
}
