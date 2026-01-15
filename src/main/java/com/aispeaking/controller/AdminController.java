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
     * POST /admin/questions/bulk-delete
     * 
     * Request JSON:
     * {
     *   "questionIds": [1, 2, 3, 4, 5]
     * }
     * 
     * Response JSON:
     * {
     *   "requested": 5,
     *   "deleted": 5
     * }
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
     * PUT /admin/exams/bulk-update-status
     * 
     * Request JSON:
     * {
     *   "examIds": [1, 2, 3],
     *   "status": "ACTIVE"  // ACTIVE, INACTIVE, DRAFT
     * }
     * 
     * Response JSON:
     * {
     *   "requested": 3,
     *   "updated": 3
     * }
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
     * GET /admin/test-sessions?status=COMPLETED&examId=1&page=0&size=10
     * 
     * Response JSON (Page):
     * {
     *   "content": [
     *     {
     *       "id": 1,
     *       "examId": 1,
     *       "examName": "English Speaking Test",
     *       "studentName": "Nguyen Van A",
     *       "studentOrganization": "University ABC",
     *       "status": "COMPLETED",
     *       "totalScore": 85.5,
     *       "startedAt": "2026-01-15T10:00:00",
     *       "completedAt": "2026-01-15T11:30:00"
     *     }
     *   ],
     *   "totalElements": 50,
     *   "totalPages": 5,
     *   "size": 10,
     *   "number": 0
     * }
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
     * PUT /admin/test-sessions/{id}/cancel
     * 
     * No request body required
     * 
     * Response: 200 OK (empty body)
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
     * DELETE /admin/test-sessions/{id}
     * 
     * No request body required
     * 
     * Response: 204 No Content
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
     * GET /admin/health
     * 
     * Response JSON:
     * {
     *   "database": "OK",
     *   "questionCount": 150,
     *   "timestamp": "2026-01-15T14:30:00",
     *   "status": "RUNNING"
     * }
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
     * GET /admin/config
     * 
     * Response JSON:
     * {
     *   "maxFileSize": "50MB",
     *   "allowedAudioFormats": ["wav", "mp3", "m4a"],
     *   "aiWhisperUrl": "http://localhost:5000",
     *   "aiQwenUrl": "http://localhost:5001"
     * }
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
