package com.aispeaking.controller;

import com.aispeaking.dto.TestSessionResponse;
import com.aispeaking.entity.TestSession;
import com.aispeaking.entity.enums.ExamStatus;
import com.aispeaking.entity.enums.TestSessionStatus;
import com.aispeaking.repository.ExamRepository;
import com.aispeaking.repository.TestSessionRepository;
import com.aispeaking.service.ExamService;
import com.aispeaking.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class AdminController {

    private final QuestionService questionService;
    private final ExamService examService;
    private final TestSessionRepository testSessionRepository;
    private final ExamRepository examRepository;

    /**
     * Bulk delete questions
     * Body: { "questionIds": [1, 2, 3] }
     */
    @DeleteMapping("/questions/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDeleteQuestions(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        var questionIds = (java.util.List<Integer>) request.get("questionIds");
        
        int deleted = 0;
        for (Integer id : questionIds) {
            try {
                questionService.deleteQuestion(id.longValue());
                deleted++;
            } catch (Exception e) {
                // Continue with next
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "requested", questionIds.size(),
            "deleted", deleted
        ));
    }

    /**
     * Bulk update exam status
     * Body: { "examIds": [1, 2], "status": "ACTIVE" }
     */
    @PutMapping("/exams/bulk-update-status")
    public ResponseEntity<Map<String, Object>> bulkUpdateExamStatus(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        var examIds = (java.util.List<Integer>) request.get("examIds");
        String statusStr = request.get("status").toString();
        ExamStatus status = ExamStatus.valueOf(statusStr);
        
        int updated = 0;
        for (Integer id : examIds) {
            try {
                var exam = examService.getExamEntityById(id.longValue());
                exam.setStatus(status);
                examRepository.save(exam);
                updated++;
            } catch (Exception e) {
                // Continue with next
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "requested", examIds.size(),
            "updated", updated
        ));
    }

    /**
     * Get all test sessions with filtering
     * Query params: status, examId
     */
    @GetMapping("/test-sessions")
    public ResponseEntity<Page<TestSessionResponse>> getAllTestSessions(
            @RequestParam(required = false) TestSessionStatus status,
            @RequestParam(required = false) Long examId,
            Pageable pageable) {
        
        Page<TestSession> sessions;
        
        if (status != null) {
            sessions = testSessionRepository.findByStatus(status, pageable);
        } else if (examId != null) {
            sessions = testSessionRepository.findByExamId(examId, pageable);
        } else {
            sessions = testSessionRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(sessions.map(TestSessionResponse::from));
    }

    /**
     * Cancel a test session
     */
    @PutMapping("/test-sessions/{id}/cancel")
    public ResponseEntity<Void> cancelTestSession(@PathVariable Long id) {
        TestSession session = testSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test session not found"));
        
        session.setStatus(TestSessionStatus.CANCELLED);
        testSessionRepository.save(session);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Delete test session (soft delete)
     */
    @DeleteMapping("/test-sessions/{id}")
    public ResponseEntity<Void> deleteTestSession(@PathVariable Long id) {
        TestSession session = testSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test session not found"));
        
        // You can implement soft delete in TestSession entity if needed
        testSessionRepository.delete(session);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database
            long questionCount = questionService.getAllQuestions(Pageable.unpaged()).getTotalElements();
            health.put("database", "OK");
            health.put("questionCount", questionCount);
        } catch (Exception e) {
            health.put("database", "ERROR: " + e.getMessage());
        }
        
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("status", "RUNNING");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get system configuration
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("maxFileSize", "50MB");
        config.put("allowedAudioFormats", new String[]{"wav", "mp3", "m4a"});
        config.put("aiWhisperUrl", "http://localhost:5000");
        config.put("aiQwenUrl", "http://localhost:5001");
        
        return ResponseEntity.ok(config);
    }
}
