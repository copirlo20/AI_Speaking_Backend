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
     * GET /statistics/dashboard
     * 
     * Response JSON:
     * {
     *   "totalQuestions": 150,
     *   "totalExams": 25,
     *   "totalTestSessions": 500,
     *   "completedTestSessions": 450,
     *   "averageScore": 75.5,
     *   "totalUsers": 50,
     *   "activeUsers": 45
     * }
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(statisticsService.getDashboardStats());
    }

    /**
     * Get question statistics by level
     * GET /statistics/questions/by-level
     * 
     * Response JSON:
     * {
     *   "EASY": 80,
     *   "HARD": 70
     * }
     */
    @GetMapping("/questions/by-level")
    public ResponseEntity<Map<String, Long>> getQuestionStatsByLevel() {
        return ResponseEntity.ok(statisticsService.getQuestionStatsByLevel());
    }

    /**
     * Get exam statistics by status
     * GET /statistics/exams/by-status
     * 
     * Response JSON:
     * {
     *   "ACTIVE": 15,
     *   "INACTIVE": 5,
     *   "DRAFT": 5
     * }
     */
    @GetMapping("/exams/by-status")
    public ResponseEntity<Map<String, Long>> getExamStatsByStatus() {
        return ResponseEntity.ok(statisticsService.getExamStatsByStatus());
    }

    /**
     * Get test session statistics by status
     * GET /statistics/test-sessions/by-status
     * 
     * Response JSON:
     * {
     *   "IN_PROGRESS": 50,
     *   "COMPLETED": 450,
     *   "CANCELLED": 5
     * }
     */
    @GetMapping("/test-sessions/by-status")
    public ResponseEntity<Map<String, Long>> getTestSessionStatsByStatus() {
        return ResponseEntity.ok(statisticsService.getTestSessionStatsByStatus());
    }

    /**
     * Get detailed statistics for a specific test session
     * GET /statistics/test-sessions/{id}
     * 
     * Response JSON:
     * {
     *   "totalQuestions": 10,
     *   "answeredQuestions": 8,
     *   "pendingQuestions": 2,
     *   "averageScore": 7.5,
     *   "maxScore": 9.5,
     *   "minScore": 5.0,
     *   "completionRate": 80.0
     * }
     */
    @GetMapping("/test-sessions/{id}")
    public ResponseEntity<Map<String, Object>> getTestSessionStats(@PathVariable Long id) {
        return ResponseEntity.ok(statisticsService.getTestSessionStats(id));
    }

    /**
     * Get detailed statistics for a specific exam
     * GET /statistics/exams/{id}
     * 
     * Response JSON:
     * {
     *   "totalAttempts": 50,
     *   "completedAttempts": 45,
     *   "completionRate": 90.0,
     *   "averageScore": 75.5,
     *   "passRate": 80.0,
     *   "maxScore": 95.0,
     *   "minScore": 50.0
     * }
     */
    @GetMapping("/exams/{id}")
    public ResponseEntity<Map<String, Object>> getExamStats(@PathVariable Long id) {
        return ResponseEntity.ok(statisticsService.getExamStats(id));
    }

    /**
     * Get recent test sessions
     * GET /statistics/test-sessions/recent?limit=10
     * 
     * Response JSON:
     * [
     *   {
     *     "sessionId": 100,
     *     "studentName": "Nguyen Van A",
     *     "examName": "English Speaking Test",
     *     "score": 85.5,
     *     "status": "COMPLETED",
     *     "completedAt": "2026-01-15T11:30:00"
     *   }
     * ]
     */
    @GetMapping("/test-sessions/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentTestSessions(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(statisticsService.getRecentTestSessions(limit));
    }

    /**
     * Get statistics by date range
     * GET /statistics/by-date-range?startDate=2026-01-01T00:00:00&endDate=2026-01-31T23:59:59
     * 
     * Response JSON:
     * {
     *   "questionsCreated": 20,
     *   "examsCreated": 5,
     *   "testsTaken": 100,
     *   "testsCompleted": 90,
     *   "averageScore": 75.5,
     *   "dateRange": {
     *     "from": "2026-01-01T00:00:00",
     *     "to": "2026-01-31T23:59:59"
     *   }
     * }
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<Map<String, Object>> getStatsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(statisticsService.getStatsByDateRange(startDate, endDate));
    }
}
