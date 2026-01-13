package com.aispeaking.controller;

import com.aispeaking.entity.TestAnswer;
import com.aispeaking.entity.TestSession;
import com.aispeaking.service.TestSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test-sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class TestSessionController {

    private final TestSessionService testSessionService;

    @PostMapping
    public ResponseEntity<TestSession> createTestSession(@RequestBody Map<String, Object> request) {
        Long examId = Long.valueOf(request.get("examId").toString());
        String studentName = request.get("studentName").toString();
        String studentOrganization = request.getOrDefault("studentOrganization", "").toString();
        String studentEmail = request.getOrDefault("studentEmail", "").toString();
        
        TestSession testSession = testSessionService.createTestSession(
                examId, studentName, studentOrganization, studentEmail);
        
        return ResponseEntity.ok(testSession);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestSession> getTestSession(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestSession(id));
    }

    @GetMapping("/{id}/answers")
    public ResponseEntity<List<TestAnswer>> getTestAnswers(@PathVariable Long id) {
        return ResponseEntity.ok(testSessionService.getTestAnswers(id));
    }

    @PostMapping("/{id}/submit-answer")
    public ResponseEntity<TestAnswer> submitAnswer(
            @PathVariable Long id,
            @RequestParam Long questionId,
            @RequestParam("audio") MultipartFile audioFile) {
        try {
            TestAnswer testAnswer = testSessionService.submitAnswer(id, questionId, audioFile);
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
