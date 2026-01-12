package com.aispeaking.service;

import com.aispeaking.dto.AIScoreResponse;
import com.aispeaking.model.Question;
import com.aispeaking.model.SpeakingTest;
import com.aispeaking.model.TestResult;
import com.aispeaking.model.User;
import com.aispeaking.repository.QuestionRepository;
import com.aispeaking.repository.SpeakingTestRepository;
import com.aispeaking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${ai.service.model}")
    private String aiModel;

    private final UserRepository userRepository;
    private final SpeakingTestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final WebClient.Builder webClientBuilder;
    private final AIAutoScoringService autoScoringService;

    private static final String UPLOAD_DIR = "uploads/audio/";

    /**
     * Process audio file and evaluate speaking performance using local AI with auto-scoring
     */
    public TestResult processAudioAndEvaluate(Long userId, Long testId, Long questionId, MultipartFile audioFile) {
        try {
            // Get entities
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            SpeakingTest test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));
            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

            // Save audio file
            String audioFilePath = saveAudioFile(audioFile);

            // Get transcription from local AI (Whisper)
            String transcription = getTranscriptionFromAI(audioFilePath);

            // Calculate audio duration (estimate from file size)
            Integer durationSeconds = estimateAudioDuration(audioFile);

            // Use advanced auto-scoring service
            AIScoreResponse scoreResponse = autoScoringService.autoScoreResponse(
                transcription, question, durationSeconds
            );

            // Create test result
            TestResult result = new TestResult();
            result.setUser(user);
            result.setTest(test);
            result.setQuestion(question);
            result.setTranscription(transcription);
            result.setAudioFilePath(audioFilePath);
            result.setRecordingDuration(durationSeconds);
            result.setPronunciationScore(scoreResponse.getPronunciationScore());
            result.setFluencyScore(scoreResponse.getFluencyScore());
            result.setGrammarScore(scoreResponse.getGrammarScore());
            result.setVocabularyScore(scoreResponse.getVocabularyScore());
            result.setContentScore(scoreResponse.getContentScore());
            result.setOverallScore(scoreResponse.getOverallScore());
            result.setAiFeedback(scoreResponse.getFeedback());
            result.setSuggestions(String.join("\n", scoreResponse.getSuggestions()));
            result.setStatus(TestResult.AnalysisStatus.COMPLETED);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error processing audio: " + e.getMessage(), e);
        }
    }

    /**
     * Estimate audio duration from file size (rough approximation)
     */
    private Integer estimateAudioDuration(MultipartFile audioFile) {
        try {
            // Rough estimation: ~1 second per 16KB for typical audio
            long fileSizeKB = audioFile.getSize() / 1024;
            return (int) Math.max(1, fileSizeKB / 16);
        } catch (Exception e) {
            return 30; // Default 30 seconds if calculation fails
        }
    }

    /**
     * Save uploaded audio file to local storage
     */
    private String saveAudioFile(MultipartFile audioFile) throws IOException {
        // Create upload directory if it doesn't exist
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Generate unique filename
        String originalFilename = audioFile.getOriginalFilename();
        String extension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".wav";
        String filename = UUID.randomUUID().toString() + extension;
        
        Path filePath = Path.of(UPLOAD_DIR + filename);
        Files.write(filePath, audioFile.getBytes());
        
        return filePath.toString();
    }

    /**
     * Get transcription from local AI service (Whisper)
     */
    private String getTranscriptionFromAI(String audioFilePath) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            // Call local Whisper API
            Map<String, Object> request = new HashMap<>();
            request.put("audio_file", audioFilePath);
            request.put("model", aiModel);

            Map<String, Object> response = webClient.post()
                .uri("/api/transcribe")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            return response != null ? (String) response.get("text") : "";
        } catch (Exception e) {
            // Fallback: return placeholder if AI service is not available
            return "[Transcription from audio - AI service offline]";
        }
    }

    /**
     * Evaluate transcription using AI to generate scores
     */
    private Map<String, Double> evaluateTranscriptionWithAI(String transcription, Question question) {
        Map<String, Double> scores = new HashMap<>();
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            Map<String, Object> request = new HashMap<>();
            request.put("transcription", transcription);
            request.put("question", question.getQuestionText());
            request.put("sample_answer", question.getSampleAnswer());
            request.put("criteria", question.getEvaluationCriteria());

            Map<String, Object> response = webClient.post()
                .uri("/api/evaluate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null) {
                scores.put("pronunciation", ((Number) response.getOrDefault("pronunciation", 0)).doubleValue());
                scores.put("fluency", ((Number) response.getOrDefault("fluency", 0)).doubleValue());
                scores.put("grammar", ((Number) response.getOrDefault("grammar", 0)).doubleValue());
                scores.put("vocabulary", ((Number) response.getOrDefault("vocabulary", 0)).doubleValue());
                scores.put("content", ((Number) response.getOrDefault("content", 0)).doubleValue());
            }
        } catch (Exception e) {
            // Fallback: generate basic scores
            scores.put("pronunciation", 7.0);
            scores.put("fluency", 7.0);
            scores.put("grammar", 7.0);
            scores.put("vocabulary", 7.0);
            scores.put("content", 7.0);
        }

        return scores;
    }

    /**
     * Generate feedback using AI
     */
    private String generateFeedbackWithAI(String transcription, Question question, Map<String, Double> scores) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            Map<String, Object> request = new HashMap<>();
            request.put("transcription", transcription);
            request.put("question", question.getQuestionText());
            request.put("scores", scores);

            Map<String, Object> response = webClient.post()
                .uri("/api/feedback")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            return response != null ? (String) response.get("feedback") : 
                "Good effort! Keep practicing to improve your speaking skills.";
        } catch (Exception e) {
            // Fallback feedback
            return "Your response has been recorded and analyzed. " +
                   "Overall score: " + scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0) + "/10. " +
                   "Keep practicing to improve your speaking skills!";
        }
    }

    /**
     * Direct transcription method for testing
     */
    public String transcribeAudio(MultipartFile audioFile) throws IOException {
        String audioFilePath = saveAudioFile(audioFile);
        return getTranscriptionFromAI(audioFilePath);
    }
}
