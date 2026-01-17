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

    /**
     * Xử lý câu trả lời bài kiểm tra bất đồng bộ (xử lý nền)
     * Lưu vào cơ sở dữ liệu sau khi xử lý hoàn tất
     */
    @Async
    @Transactional
    public void processTestAnswer(TestAnswer testAnswer) {
        // Xử lý đồng bộ (cập nhật đối tượng)
        processTestAnswerSync(testAnswer);
        
        // Lưu vào cơ sở dữ liệu (ngữ cảnh bất đồng bộ, giao dịch riêng biệt)
        testAnswerRepository.save(testAnswer);
        
        log.info("Async processing completed and saved for test answer {}", testAnswer.getId());
    }

    /**
     * Xử lý câu trả lời bài kiểm tra đồng bộ (chặn cho đến khi hoàn thành)
     * QUAN TRỌNG: Phương thức này KHÔNG lưu vào cơ sở dữ liệu. Nó chỉ cập nhật đối tượng TestAnswer.
     * Người gọi có trách nhiệm lưu vào cơ sở dữ liệu sau khi phương thức này hoàn thành.
     * Điều này đảm bảo giao dịch nguyên tử - chỉ lưu một lần sau khi tất cả quá trình xử lý hoàn tất.
     */
    public void processTestAnswerSync(TestAnswer testAnswer) {
        log.info("Starting AI processing for test answer {}", testAnswer.getId());
        
        try {
            // Bước 1: Chuyển đổi âm thanh thành văn bản với Whisper
            testAnswer.setProcessingStatus(ProcessingStatus.TRANSCRIBING);
            log.info("Test answer {} - Status: TRANSCRIBING", testAnswer.getId());
            
            String transcribedText = transcribeAudio(testAnswer);
            testAnswer.setTranscribedText(transcribedText);
            log.info("Test answer {} - Transcription completed: {}", testAnswer.getId(), 
                    transcribedText.substring(0, Math.min(50, transcribedText.length())));
            
            // Bước 2: Chấm điểm với Qwen
            testAnswer.setProcessingStatus(ProcessingStatus.SCORING);
            log.info("Test answer {} - Status: SCORING", testAnswer.getId());
            
            Map<String, Object> scoringResult = scoreAnswer(testAnswer);
            
            BigDecimal score = new BigDecimal(scoringResult.get("score").toString());
            String feedback = scoringResult.get("feedback").toString();
            
            testAnswer.setScore(score);
            testAnswer.setFeedback(feedback);
            testAnswer.setProcessingStatus(ProcessingStatus.COMPLETED);
            
            log.info("Completed AI processing for test answer {} - Score: {}, Status: COMPLETED", 
                    testAnswer.getId(), score);
            
        } catch (Exception e) {
            log.error("Error processing test answer {}: {}", testAnswer.getId(), e.getMessage(), e);
            testAnswer.setProcessingStatus(ProcessingStatus.FAILED);
            testAnswer.setFeedback("Lỗi xử lý: " + e.getMessage());
            log.warn("Test answer {} - Status: FAILED", testAnswer.getId());
        }
    }

    private String transcribeAudio(TestAnswer testAnswer) throws Exception {
        long startTime = System.currentTimeMillis();
        
        log.info("Transcribing audio for test answer {}, file: {}", testAnswer.getId(), testAnswer.getAudioUrl());
        
        File audioFile = new File(testAnswer.getAudioUrl());
        if (!audioFile.exists()) {
            throw new RuntimeException("Audio file not found: " + testAnswer.getAudioUrl());
        }
        
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
        log.info("Audio file size: {} bytes", audioBytes.length);
        
        WebClient webClient = webClientBuilder.baseUrl(whisperUrl).build();
        
        Map<String, Object> request = new HashMap<>();
        request.put("audio_data", audioBytes);
        
        // Tạo metadata cho việc ghi log (không có dữ liệu âm thanh để tránh tràn cơ sở dữ liệu)
        Map<String, Object> requestMetadata = new HashMap<>();
        requestMetadata.put("audio_file", testAnswer.getAudioUrl());
        requestMetadata.put("audio_size_bytes", audioBytes.length);
        String requestJson = objectMapper.writeValueAsString(requestMetadata);
        
        log.info("Sending transcribe request to Whisper at {}/transcribe", whisperUrl);
        
        String response;
        try {
            response = webClient.post()
                    .uri("/transcribe")
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError(),
                        clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    log.error("Whisper API returned 4xx error. Status: {}, Body: {}", 
                                            clientResponse.statusCode(), errorBody);
                                    return new RuntimeException("Whisper API error (4xx): " + errorBody);
                                });
                        }
                    )
                    .onStatus(
                        status -> status.is5xxServerError(),
                        clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    log.error("Whisper API returned 5xx error. Status: {}, Body: {}", 
                                            clientResponse.statusCode(), errorBody);
                                    return new RuntimeException("Whisper API error (5xx): " + errorBody);
                                });
                        }
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
        } catch (Exception e) {
            log.error("Error calling Whisper API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to transcribe audio with Whisper: " + e.getMessage(), e);
        }
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        log.info("Whisper response received in {}ms", processingTime);
        log.info("Whisper raw response: {}", response);
        
        // Ghi log request/response
        logAIRequest(testAnswer, AIServiceType.WHISPER, requestJson, response, (int) processingTime, null);
        
        JsonNode jsonNode = objectMapper.readTree(response);
        
        // Kiểm tra xem trường transcribedText có tồn tại không
        if (!jsonNode.has("transcribedText")) {
            throw new RuntimeException("Whisper response missing 'transcribedText' field. Response: " + response);
        }
        
        String transcribedText = jsonNode.get("transcribedText").asText();
        
        // Kiểm tra văn bản chuyển đổi không được rỗng
        if (transcribedText == null || transcribedText.trim().isEmpty()) {
            log.warn("Whisper returned empty text! Full response: {}", response);
            throw new RuntimeException("Whisper returned empty transcription. The audio may be silent or corrupted.");
        }
        
        log.info("Transcribed text (length {} chars): {}", transcribedText.length(), transcribedText);
        
        return transcribedText;
    }

    private Map<String, Object> scoreAnswer(TestAnswer testAnswer) throws Exception {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting Qwen scoring for test answer {}", testAnswer.getId());
        
        // Kiểm tra văn bản đã chuyển đổi
        String transcribedText = testAnswer.getTranscribedText();
        if (transcribedText == null || transcribedText.trim().isEmpty()) {
            String errorMsg = "Cannot score: transcribed text is empty. Whisper may have failed to transcribe the audio.";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        
        log.info("Transcribed text to score (length: {} chars): {}", 
                transcribedText.length(), 
                transcribedText.substring(0, Math.min(100, transcribedText.length())));
        
        // Lấy các câu trả lời mẫu cho câu hỏi
        List<SampleAnswer> sampleAnswers = sampleAnswerRepository
                .findByQuestionId(testAnswer.getQuestion().getId());
        
        log.info("Found {} sample answers for question {}", sampleAnswers.size(), testAnswer.getQuestion().getId());
        
        WebClient webClient = webClientBuilder.baseUrl(qwenUrl).build();
        
        // Xây dựng request theo định dạng mà qwen_server mong đợi
        Map<String, Object> request = new HashMap<>();
        request.put("question", testAnswer.getQuestion().getContent());
        request.put("transcribedText", transcribedText);
        
        // Thêm các câu trả lời mẫu theo định dạng mong đợi
        List<Map<String, Object>> sampleList = sampleAnswers.stream()
                .map(sample -> {
                    Map<String, Object> sampleMap = new HashMap<>();
                    sampleMap.put("content", sample.getContent());  // Qwen mong đợi "content" không phải "text"
                    sampleMap.put("score", sample.getScore());
                    return sampleMap;
                })
                .toList();
        request.put("sample_answers", sampleList);
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        log.info("Sending score request to Qwen at {}/score", qwenUrl);
        log.info("Request JSON payload:");
        log.info("  - question: {}", request.get("question"));
        log.info("  - transcribedText length: {} chars", transcribedText.length());
        log.info("  - sample_answers count: {}", sampleList.size());
        if (!sampleList.isEmpty()) {
            log.info("  - first sample: content='{}', score={}", 
                    sampleList.get(0).get("content"), 
                    sampleList.get(0).get("score"));
        }
        log.debug("Full request JSON: {}", requestJson);
        
        try {
            String response = webClient.post()
                    .uri("/score")
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError(),
                        clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    log.error("Qwen API returned 4xx error. Status: {}, Body: {}", 
                                            clientResponse.statusCode(), errorBody);
                                    return new RuntimeException("Qwen API error (4xx): " + errorBody);
                                });
                        }
                    )
                    .onStatus(
                        status -> status.is5xxServerError(),
                        clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    log.error("Qwen API returned 5xx error. Status: {}, Body: {}", 
                                            clientResponse.statusCode(), errorBody);
                                    return new RuntimeException("Qwen API error (5xx): " + errorBody);
                                });
                        }
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Qwen response received in {}ms: {}", processingTime, response);
            
            // Ghi log request/response
            logAIRequest(testAnswer, AIServiceType.QWEN, requestJson, response, (int) processingTime, null);
            
            JsonNode jsonNode = objectMapper.readTree(response);
            
            Map<String, Object> result = new HashMap<>();
            result.put("score", jsonNode.get("score").asDouble());
            result.put("feedback", jsonNode.get("feedback").asText());
            
            log.info("Qwen scoring completed: score={}, feedback={}", result.get("score"), result.get("feedback"));
            
            return result;
        } catch (Exception e) {
            log.error("Error calling Qwen API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to score answer with Qwen: " + e.getMessage(), e);
        }
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
