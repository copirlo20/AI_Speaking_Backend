package com.aispeaking.controller;

import com.aispeaking.dto.*;
import com.aispeaking.entity.enums.TestSessionStatus;
import com.aispeaking.service.TestSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/test-sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class TestSessionController {

    private final TestSessionService testSessionService;

    /**
     * Get all test sessions with pagination
     * GET /test-sessions?page=0&size=10&sort=id,desc
     * 
     * Response JSON: Same as AdminController getAllTestSessions
     */
    @GetMapping
    public ResponseEntity<Page<TestSessionResponse>> getAllTestSessions(Pageable pageable) {
        return ResponseEntity.ok(testSessionService.getAllTestSessions(pageable));
    }

    /**
     * Search test sessions with filters
     * GET /test-sessions/search?examId=1&studentName=Nguyen&status=COMPLETED
     * 
     * Response JSON: Same as getAllTestSessions
     */
    @GetMapping("/search")
    public ResponseEntity<Page<TestSessionResponse>> searchTestSessions(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) TestSessionStatus status,
            @RequestParam(required = false) BigDecimal minScore,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        return ResponseEntity.ok(testSessionService.searchTestSessions(
                examId, studentName, status, minScore, maxScore, fromDate, toDate, pageable));
    }

    /**
     * Create new test session
     * POST /test-sessions
     * 
     * Request JSON:
     * {
     *   "examId": 1,
     *   "studentName": "Nguyen Van A",
     *   "studentOrganization": "University ABC"
     * }
     * 
     * Response JSON:
     * {
     *   "id": 1,
     *   "examId": 1,
     *   "examName": "English Speaking Test",
     *   "studentName": "Nguyen Van A",
     *   "studentOrganization": "University ABC",
     *   "status": "IN_PROGRESS",
     *   "totalScore": 0,
     *   "startedAt": "2026-01-15T14:30:00",
     *   "completedAt": null
     * }
     */
    @PostMapping
    public ResponseEntity<TestSessionResponse> createTestSession(@Valid @RequestBody CreateTestSessionRequest request) {
        TestSessionResponse testSession = testSessionService.createTestSession(
                request.getExamId(), 
                request.getStudentName(), 
                request.getStudentOrganization());
        
        return ResponseEntity.ok(testSession);
    }

    /**
     * Get test session by ID
     * GET /test-sessions/{id}
     * 
     * Response JSON: Same as createTestSession
     */
    @GetMapping("/{id}")
    public ResponseEntity<TestSessionResponse> getTestSession(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestSession(id));
    }

    /**
     * Get all answers for a test session
     * GET /test-sessions/{id}/answers
     * 
     * Response JSON:
     * [
     *   {
     *     "id": 1,
     *     "testSessionId": 1,
     *     "questionId": 5,
     *     "questionContent": "Describe your hometown",
     *     "transcribedText": "My hometown is Ha Noi...",
     *     "score": 8.5,
     *     "feedback": "Good pronunciation and grammar",
     *     "processingStatus": "COMPLETED",
     *     "answeredAt": "2026-01-15T14:35:00"
     *   }
     * ]
     */
    @GetMapping("/{id}/answers")
    public ResponseEntity<List<TestAnswerResponse>> getTestAnswers(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestAnswers(id));
    }

    /**
     * Submit answer with audio file
     * POST /test-sessions/{id}/submit-answer
     * 
     * Form Data:
     * - questionId: Long (form field)
     * - audio: MultipartFile (audio file: wav, mp3, m4a)
     * 
     * Response JSON: Single TestAnswerResponse (same as getTestAnswers item)
     */
    @PostMapping("/{id}/submit-answer")
    public ResponseEntity<TestAnswerResponse> submitAnswer(
            @PathVariable Long id,
            @RequestParam Long questionId,
            @RequestParam("audio") MultipartFile audioFile) {
        try {
            TestAnswerResponse testAnswer = testSessionService.submitAnswer(id, questionId, audioFile);
            return ResponseEntity.ok(testAnswer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Complete test session
     * POST /test-sessions/{id}/complete
     * 
     * Response: 200 OK (empty body)
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTestSession(@PathVariable Long id) {
        testSessionService.completeTestSession(id);
        return ResponseEntity.ok().build();
    }
}
