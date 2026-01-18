package com.aispeaking.controller;

import com.aispeaking.entity.TestAnswer;
import com.aispeaking.entity.TestSession;
import com.aispeaking.repository.TestAnswerRepository;
import com.aispeaking.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class ReportController {
    private final TestSessionRepository testSessionRepository;
    private final TestAnswerRepository testAnswerRepository;

    /**
     * Xuất báo cáo phiên thi dưới dạng CSV
     * GET /reports/test-session/{id}/export-csv
     * 
     * Response: CSV file download
     * Content-Type: text/csv
     * Content-Disposition: attachment; filename="test-session-{id}.csv"
     * 
     * CSV Format:
     * Question,Transcribed Text,Score,Feedback,Status
     * "Describe your hometown","My hometown is Ha Noi...",8.5,"Good pronunciation",COMPLETED
     */
    @GetMapping("/test-session/{id}/export-csv")
    public ResponseEntity<String> exportTestSessionCsv(@PathVariable Long id) {
        List<TestAnswer> answers = testAnswerRepository.findByTestSessionId(id);
        StringBuilder csv = new StringBuilder();
        csv.append("Question,Transcribed Text,Score,Feedback,Status\n");
        for (TestAnswer answer : answers) {
            csv.append("\"").append(answer.getQuestion().getContent().replace("\"", "\"\"")).append("\",");
            csv.append("\"").append(answer.getTranscribedText() != null ? answer.getTranscribedText().replace("\"", "\"\"") : "").append("\",");
            csv.append(answer.getScore()).append(",");
            csv.append("\"").append(answer.getFeedback() != null ? answer.getFeedback().replace("\"", "\"\"") : "").append("\",");
            csv.append(answer.getProcessingStatus().name()).append("\n");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "test-session-" + id + ".csv");
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
    }

    /**
     * Lấy báo cáo chi tiết phiên thi
     * GET /reports/test-session/{id}/detailed
     * 
     * Response JSON:
     * {
     *   "sessionId": 1,
     *   "studentName": "Nguyen Van A",
     *   "studentOrganization": "University ABC",
     *   "examName": "English Speaking Test",
     *   "totalScore": 85.5,
     *   "status": "COMPLETED",
     *   "startedAt": "2026-01-15T10:00:00",
     *   "completedAt": "2026-01-15T11:30:00",
     *   "answers": [
     *     {
     *       "questionId": 5,
     *       "questionContent": "Describe your hometown",
     *       "questionLevel": "EASY",
     *       "transcribedText": "My hometown is Ha Noi...",
     *       "score": 8.5,
     *       "feedback": "Good pronunciation and grammar",
     *       "status": "COMPLETED",
     *       "answeredAt": "2026-01-15T10:15:00"
     *     }
     *   ],
     *   "completedAnswers": 10,
     *   "totalQuestions": 10,
     *   "completionRate": 100.0
     * }
     */
    @GetMapping("/test-session/{id}/detailed")
    public ResponseEntity<java.util.Map<String, Object>> getDetailedTestSessionReport(@PathVariable Long id) {
        TestSession session = testSessionRepository.findById(id).orElseThrow(() -> new RuntimeException("Test session not found"));
        List<TestAnswer> answers = testAnswerRepository.findByTestSessionId(id);
        java.util.Map<String, Object> report = new java.util.HashMap<>();
        // Thông tin phiên
        report.put("sessionId", session.getId());
        report.put("studentName", session.getStudentName());
        report.put("studentOrganization", session.getStudentOrganization());
        report.put("examName", session.getExam().getName());
        report.put("totalScore", session.getTotalScore());
        report.put("status", session.getStatus().name());
        report.put("startedAt", session.getStartedAt());
        report.put("completedAt", session.getCompletedAt());
        // Chi tiết câu trả lời
        List<java.util.Map<String, Object>> answerDetails = answers.stream()
                .map(answer -> {
                    java.util.Map<String, Object> detail = new java.util.HashMap<>();
                    detail.put("questionId", answer.getQuestion().getId());
                    detail.put("questionContent", answer.getQuestion().getContent());
                    detail.put("questionLevel", answer.getQuestion().getLevel().name());
                    detail.put("transcribedText", answer.getTranscribedText());
                    detail.put("score", answer.getScore());
                    detail.put("feedback", answer.getFeedback());
                    detail.put("status", answer.getProcessingStatus().name());
                    detail.put("answeredAt", answer.getAnsweredAt());
                    return detail;
                })
                .toList();
        report.put("answers", answerDetails);
        // Thống kê
        long completed = answers.stream().filter(a -> a.getProcessingStatus().name().equals("COMPLETED")).count();
        report.put("completedAnswers", completed);
        report.put("totalQuestions", answers.size());
        report.put("completionRate", answers.isEmpty() ? 0 : (completed * 100.0 / answers.size()));
        return ResponseEntity.ok(report);
    }

    /**
     * Xuất báo cáo phiên thi của một kỳ thi dưới dạng CSV
     * GET /reports/exam/{examId}/export-csv
     * 
     * Response: CSV file download
     * Content-Type: text/csv
     * Content-Disposition: attachment; filename="exam-{examId}-sessions.csv"
     * 
     * CSV Format:
     * Session ID,Student Name,Organization,Total Score,Status,Started At,Completed At
     * 1,"Nguyen Van A","University ABC",85.5,COMPLETED,2026-01-15 10:00:00,2026-01-15 11:30:00
     */
    @GetMapping("/exam/{examId}/export-csv")
    public ResponseEntity<String> exportExamSessionsCsv(@PathVariable Long examId) {
        List<TestSession> sessions = testSessionRepository.findAll().stream().filter(ts -> ts.getExam().getId().equals(examId)).toList();
        StringBuilder csv = new StringBuilder();
        csv.append("Session ID,Student Name,Organization,Total Score,Status,Started At,Completed At\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TestSession session : sessions) {
            csv.append(session.getId()).append(",");
            csv.append("\"").append(session.getStudentName()).append("\",");
            csv.append("\"").append(session.getStudentOrganization() != null ? session.getStudentOrganization() : "").append("\",");
            csv.append(session.getTotalScore()).append(",");
            csv.append(session.getStatus().name()).append(",");
            csv.append(session.getStartedAt().format(formatter)).append(",");
            csv.append(session.getCompletedAt() != null ? session.getCompletedAt().format(formatter) : "").append("\n");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "exam-" + examId + "-sessions.csv");
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
    }
}