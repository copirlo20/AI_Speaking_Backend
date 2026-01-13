package com.aispeaking.controller;

import com.aispeaking.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Get dashboard statistics
     * Returns: Total questions, exams, test sessions, average scores, etc.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(statisticsService.getDashboardStats());
    }

    /**
     * Get question statistics by level
     * Returns: Count of questions for each level (EASY, HARD)
     */
    @GetMapping("/questions/by-level")
    public ResponseEntity<Map<String, Long>> getQuestionStatsByLevel() {
        return ResponseEntity.ok(statisticsService.getQuestionStatsByLevel());
    }

    /**
     * Get exam statistics by status
     * Returns: Count of exams for each status (ACTIVE, INACTIVE, DRAFT)
     */
    @GetMapping("/exams/by-status")
    public ResponseEntity<Map<String, Long>> getExamStatsByStatus() {
        return ResponseEntity.ok(statisticsService.getExamStatsByStatus());
    }

    /**
     * Get test session statistics by status
     * Returns: Count of test sessions for each status
     */
    @GetMapping("/test-sessions/by-status")
    public ResponseEntity<Map<String, Long>> getTestSessionStatsByStatus() {
        return ResponseEntity.ok(statisticsService.getTestSessionStatsByStatus());
    }

    /**
     * Get detailed statistics for a specific test session
     * Returns: Total/answered/pending questions, average/max/min scores
     */
    @GetMapping("/test-sessions/{id}")
    public ResponseEntity<Map<String, Object>> getTestSessionStats(@PathVariable Long id) {
        return ResponseEntity.ok(statisticsService.getTestSessionStats(id));
    }

    /**
     * Get detailed statistics for a specific exam
     * Returns: Total attempts, completion rate, average score, pass rate
     */
    @GetMapping("/exams/{id}")
    public ResponseEntity<Map<String, Object>> getExamStats(@PathVariable Long id) {
        return ResponseEntity.ok(statisticsService.getExamStats(id));
    }

    /**
     * Get recent test sessions
     * Query params: limit (default: 10)
     */
    @GetMapping("/test-sessions/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentTestSessions(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(statisticsService.getRecentTestSessions(limit));
    }

    /**
     * Get statistics by date range
     * Query params: startDate, endDate (ISO format)
     * Returns: Questions created, exams created, tests taken/completed in the range
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<Map<String, Object>> getStatsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(statisticsService.getStatsByDateRange(startDate, endDate));
    }
}
