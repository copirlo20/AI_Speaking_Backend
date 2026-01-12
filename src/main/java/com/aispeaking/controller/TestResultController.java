package com.aispeaking.controller;

import com.aispeaking.dto.TestSubmissionRequest;
import com.aispeaking.model.TestResult;
import com.aispeaking.service.TestResultService;
import com.aispeaking.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TestResultController {

    private final TestResultService testResultService;
    private final AIService aiService;

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitTestResult(
            @RequestParam("userId") Long userId,
            @RequestParam("testId") Long testId,
            @RequestParam("questionId") Long questionId,
            @RequestParam("audioFile") MultipartFile audioFile) {
        try {
            // Process audio with AI service
            TestResult result = aiService.processAudioAndEvaluate(userId, testId, questionId, audioFile);
            TestResult savedResult = testResultService.createTestResult(result);
            return ResponseEntity.ok(savedResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing audio: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<TestResult>> getAllResults() {
        return ResponseEntity.ok(testResultService.getAllTestResults());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResultById(@PathVariable Long id) {
        return testResultService.getTestResultById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TestResult>> getResultsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(testResultService.getTestResultsByUserId(userId));
    }

    @GetMapping("/test/{testId}")
    public ResponseEntity<List<TestResult>> getResultsByTestId(@PathVariable Long testId) {
        return ResponseEntity.ok(testResultService.getTestResultsByTestId(testId));
    }

    @GetMapping("/user/{userId}/test/{testId}")
    public ResponseEntity<List<TestResult>> getResultsByUserAndTest(
            @PathVariable Long userId, 
            @PathVariable Long testId) {
        return ResponseEntity.ok(testResultService.getTestResultsByUserAndTest(userId, testId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResult(@PathVariable Long id) {
        try {
            testResultService.deleteTestResult(id);
            return ResponseEntity.ok().body("Result deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
