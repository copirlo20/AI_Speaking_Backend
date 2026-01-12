package com.aispeaking.controller;

import com.aispeaking.model.SpeakingTest;
import com.aispeaking.service.SpeakingTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SpeakingTestController {

    private final SpeakingTestService testService;

    @PostMapping
    public ResponseEntity<SpeakingTest> createTest(@Valid @RequestBody SpeakingTest test) {
        SpeakingTest createdTest = testService.createTest(test);
        return ResponseEntity.ok(createdTest);
    }

    @GetMapping
    public ResponseEntity<List<SpeakingTest>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests());
    }

    @GetMapping("/active")
    public ResponseEntity<List<SpeakingTest>> getActiveTests() {
        return ResponseEntity.ok(testService.getActiveTests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTestById(@PathVariable Long id) {
        return testService.getTestById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<SpeakingTest>> getTestsByLevel(@PathVariable SpeakingTest.TestLevel level) {
        return ResponseEntity.ok(testService.getTestsByLevel(level));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTest(@PathVariable Long id, @Valid @RequestBody SpeakingTest testDetails) {
        try {
            SpeakingTest updatedTest = testService.updateTest(id, testDetails);
            return ResponseEntity.ok(updatedTest);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTest(@PathVariable Long id) {
        try {
            testService.deleteTest(id);
            return ResponseEntity.ok().body("Test deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startTest(@PathVariable Long id) {
        try {
            testService.incrementAttemptCount(id);
            return ResponseEntity.ok().body("Test started successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
