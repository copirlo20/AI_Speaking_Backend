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
     * Export test session report as CSV
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
     * Generate detailed report for test session
     */
    @GetMapping("/test-session/{id}/detailed")
    public ResponseEntity<java.util.Map<String, Object>> getDetailedTestSessionReport(@PathVariable Long id) {
        TestSession session = testSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test session not found"));
        
        List<TestAnswer> answers = testAnswerRepository.findByTestSessionId(id);
        
        java.util.Map<String, Object> report = new java.util.HashMap<>();
        
        // Session info
        report.put("sessionId", session.getId());
        report.put("studentName", session.getStudentName());
        report.put("studentOrganization", session.getStudentOrganization());
        report.put("examName", session.getExam().getName());
        report.put("totalScore", session.getTotalScore());
        report.put("status", session.getStatus().name());
        report.put("startedAt", session.getStartedAt());
        report.put("completedAt", session.getCompletedAt());
        
        // Answer details
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
        
        // Statistics
        long completed = answers.stream()
                .filter(a -> a.getProcessingStatus().name().equals("COMPLETED"))
                .count();
        report.put("completedAnswers", completed);
        report.put("totalQuestions", answers.size());
        report.put("completionRate", answers.isEmpty() ? 0 : (completed * 100.0 / answers.size()));
        
        return ResponseEntity.ok(report);
    }

    /**
     * Export all test sessions for an exam as CSV
     */
    @GetMapping("/exam/{examId}/export-csv")
    public ResponseEntity<String> exportExamSessionsCsv(@PathVariable Long examId) {
        List<TestSession> sessions = testSessionRepository.findAll().stream()
                .filter(ts -> ts.getExam().getId().equals(examId))
                .toList();
        
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
