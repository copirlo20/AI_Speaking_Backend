package com.aispeaking.service;

import com.aispeaking.entity.*;
import com.aispeaking.entity.enums.AIServiceType;
import com.aispeaking.entity.enums.ProcessingStatus;
import com.aispeaking.repository.AIProcessingLogRepository;
import com.aispeaking.repository.SampleAnswerRepository;
import com.aispeaking.repository.TestAnswerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIProcessingService {

    private final TestAnswerRepository testAnswerRepository;
    private final AIProcessingLogRepository logRepository;
    private final SampleAnswerRepository sampleAnswerRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ai.whisper.url}")
    private String whisperUrl;

    @Value("${ai.qwen.url}")
    private String qwenUrl;

    @Value("${ai.request.timeout}")
    private long timeout;

    @Async
    @Transactional
    public void processTestAnswer(TestAnswer testAnswer) {
        log.info("Starting AI processing for test answer {}", testAnswer.getId());
        
        try {
            // Step 1: Transcribe audio with Whisper
            testAnswer.setProcessingStatus(ProcessingStatus.TRANSCRIBING);
            testAnswerRepository.save(testAnswer);
            
            String transcribedText = transcribeAudio(testAnswer);
            testAnswer.setTranscribedText(transcribedText);
            
            // Step 2: Score with Qwen
            testAnswer.setProcessingStatus(ProcessingStatus.SCORING);
            testAnswerRepository.save(testAnswer);
            
            Map<String, Object> scoringResult = scoreAnswer(testAnswer);
            
            BigDecimal score = new BigDecimal(scoringResult.get("score").toString());
            String feedback = scoringResult.get("feedback").toString();
            
            testAnswer.setScore(score);
            testAnswer.setFeedback(feedback);
            testAnswer.setProcessingStatus(ProcessingStatus.COMPLETED);
            testAnswerRepository.save(testAnswer);
            
            log.info("Completed AI processing for test answer {} with score {}", testAnswer.getId(), score);
            
        } catch (Exception e) {
            log.error("Error processing test answer {}: {}", testAnswer.getId(), e.getMessage(), e);
            testAnswer.setProcessingStatus(ProcessingStatus.FAILED);
            testAnswer.setFeedback("Lỗi xử lý: " + e.getMessage());
            testAnswerRepository.save(testAnswer);
        }
    }

    private String transcribeAudio(TestAnswer testAnswer) throws Exception {
        long startTime = System.currentTimeMillis();
        
        File audioFile = new File(testAnswer.getAudioUrl());
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
        
        WebClient webClient = webClientBuilder.baseUrl(whisperUrl).build();
        
        Map<String, Object> request = new HashMap<>();
        request.put("audio_data", audioBytes);
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        String response = webClient.post()
                .uri("/transcribe")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .block();
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Log the request/response
        logAIRequest(testAnswer, AIServiceType.WHISPER, requestJson, response, (int) processingTime, null);
        
        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("text").asText();
    }

    private Map<String, Object> scoreAnswer(TestAnswer testAnswer) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Get sample answers for the question
        List<SampleAnswer> sampleAnswers = sampleAnswerRepository
                .findByQuestionId(testAnswer.getQuestion().getId());
        
        WebClient webClient = webClientBuilder.baseUrl(qwenUrl).build();
        
        // Build request matching qwen_server's expected format
        Map<String, Object> request = new HashMap<>();
        request.put("question", testAnswer.getQuestion().getContent());
        request.put("user_text", testAnswer.getTranscribedText());
        
        // Add sample answers in the expected format
        List<Map<String, Object>> sampleList = sampleAnswers.stream()
                .map(sample -> {
                    Map<String, Object> sampleMap = new HashMap<>();
                    sampleMap.put("text", sample.getContent());
                    sampleMap.put("score", sample.getScore());
                    return sampleMap;
                })
                .toList();
        request.put("sample_answers", sampleList);
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        String response = webClient.post()
                .uri("/score")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .block();
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Log the request/response
        logAIRequest(testAnswer, AIServiceType.QWEN, requestJson, response, (int) processingTime, null);
        
        JsonNode jsonNode = objectMapper.readTree(response);
        
        Map<String, Object> result = new HashMap<>();
        result.put("score", jsonNode.get("score").asDouble());
        result.put("feedback", jsonNode.get("feedback").asText());
        
        return result;
    }

    private void logAIRequest(TestAnswer testAnswer, AIServiceType serviceType, 
                             String request, String response, Integer processingTime, String error) {
        AIProcessingLog log = new AIProcessingLog();
        log.setTestAnswer(testAnswer);
        log.setServiceType(serviceType);
        log.setRequestData(request);
        log.setResponseData(response);
        log.setProcessingTimeMs(processingTime);
        log.setErrorMessage(error);
        logRepository.save(log);
    }
}
