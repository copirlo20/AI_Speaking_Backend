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

    @GetMapping
    public ResponseEntity<Page<TestSessionResponse>> getAllTestSessions(Pageable pageable) {
        return ResponseEntity.ok(testSessionService.getAllTestSessions(pageable));
    }

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

    @PostMapping
    public ResponseEntity<TestSessionResponse> createTestSession(@Valid @RequestBody CreateTestSessionRequest request) {
        TestSessionResponse testSession = testSessionService.createTestSession(
                request.getExamId(), 
                request.getStudentName(), 
                request.getStudentOrganization(), 
                request.getStudentEmail());
        
        return ResponseEntity.ok(testSession);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestSessionResponse> getTestSession(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestSession(id));
    }

    @GetMapping("/{id}/answers")
    public ResponseEntity<List<TestAnswerResponse>> getTestAnswers(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestAnswers(id));
    }

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

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTestSession(@PathVariable Long id) {
        testSessionService.completeTestSession(id);
        return ResponseEntity.ok().build();
    }
}
