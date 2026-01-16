package com.aispeaking.service;

import com.aispeaking.entity.enums.ExamStatus;
import com.aispeaking.entity.enums.ProcessingStatus;
import com.aispeaking.entity.enums.QuestionLevel;
import com.aispeaking.entity.enums.TestSessionStatus;
import com.aispeaking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

        private final QuestionRepository questionRepository;
        private final ExamRepository examRepository;
        private final TestSessionRepository testSessionRepository;
        private final TestAnswerRepository testAnswerRepository;
        private final UserRepository userRepository;

        @Transactional(readOnly = true)
        public Map<String, Object> getDashboardStats() {
                Map<String, Object> stats = new HashMap<>();
                
                // Total counts
                stats.put("totalQuestions", questionRepository.findAll().stream()
                        .count());
                
                stats.put("totalExams", examRepository.findAll().stream()
                        .count());
                
                stats.put("activeExams", examRepository.findAll().stream()
                        .filter(e -> e.getStatus() == ExamStatus.ACTIVE)
                        .count());
                
                stats.put("totalTestSessions", testSessionRepository.count());
                
                stats.put("completedSessions", testSessionRepository.findAll().stream()
                        .filter(ts -> ts.getStatus() == TestSessionStatus.COMPLETED)
                        .count());
                
                stats.put("activeUsers", userRepository.findAll().stream()
                        .filter(u -> u.getIsActive())
                        .count());
                
                // Average scores
                List<BigDecimal> scores = testSessionRepository.findAll().stream()
                        .filter(ts -> ts.getStatus() == TestSessionStatus.COMPLETED)
                        .map(ts -> ts.getTotalScore())
                        .filter(score -> score != null)
                        .collect(Collectors.toList());
                
                if (!scores.isEmpty()) {
                BigDecimal average = scores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
                stats.put("averageScore", average);
                } else {
                stats.put("averageScore", 0.0);
                }
                
                return stats;
        }

        @Transactional(readOnly = true)
        public Map<String, Long> getQuestionStatsByLevel() {
                Map<String, Long> stats = new HashMap<>();
                
                for (QuestionLevel level : QuestionLevel.values()) {
                long count = questionRepository.findAll().stream()
                        .filter(q -> q.getLevel() == level)
                        .count();
                stats.put(level.name(), count);
                }
                
                return stats;
        }

        @Transactional(readOnly = true)
        public Map<String, Long> getExamStatsByStatus() {
                Map<String, Long> stats = new HashMap<>();
                
                for (ExamStatus status : ExamStatus.values()) {
                long count = examRepository.findAll().stream()
                        .filter(e -> e.getStatus() == status)
                        .count();
                stats.put(status.name(), count);
                }
                
                return stats;
        }

        @Transactional(readOnly = true)
        public Map<String, Long> getTestSessionStatsByStatus() {
                Map<String, Long> stats = new HashMap<>();
                
                for (TestSessionStatus status : TestSessionStatus.values()) {
                long count = testSessionRepository.findAll().stream()
                        .filter(ts -> ts.getStatus() == status)
                        .count();
                stats.put(status.name(), count);
                }
                
                return stats;
        }

        @Transactional(readOnly = true)
        public Map<String, Object> getTestSessionStats(Long testSessionId) {
                Map<String, Object> stats = new HashMap<>();
                
                var testAnswers = testAnswerRepository.findByTestSessionId(testSessionId);
                
                stats.put("totalQuestions", testAnswers.size());
                
                long answered = testAnswers.stream()
                        .filter(ta -> ta.getProcessingStatus() == ProcessingStatus.COMPLETED)
                        .count();
                stats.put("answeredQuestions", answered);
                
                long pending = testAnswers.stream()
                        .filter(ta -> ta.getProcessingStatus() == ProcessingStatus.PENDING 
                                || ta.getProcessingStatus() == ProcessingStatus.TRANSCRIBING
                                || ta.getProcessingStatus() == ProcessingStatus.SCORING)
                        .count();
                stats.put("pendingQuestions", pending);
                
                long failed = testAnswers.stream()
                        .filter(ta -> ta.getProcessingStatus() == ProcessingStatus.FAILED)
                        .count();
                stats.put("failedQuestions", failed);
                
                List<BigDecimal> scores = testAnswers.stream()
                        .filter(ta -> ta.getProcessingStatus() == ProcessingStatus.COMPLETED)
                        .map(ta -> ta.getScore())
                        .collect(Collectors.toList());
                
                if (!scores.isEmpty()) {
                BigDecimal average = scores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
                stats.put("averageScore", average);
                
                BigDecimal max = scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal min = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                
                stats.put("maxScore", max);
                stats.put("minScore", min);
                } else {
                stats.put("averageScore", 0.0);
                stats.put("maxScore", 0.0);
                stats.put("minScore", 0.0);
                }
                
                return stats;
        }

        @Transactional(readOnly = true)
        public Map<String, Object> getExamStats(Long examId) {
                Map<String, Object> stats = new HashMap<>();
                
                var exam = examRepository.findById(examId)
                        .orElseThrow(() -> new RuntimeException("Exam not found"));
                
                stats.put("examId", examId);
                stats.put("examName", exam.getName());
                stats.put("totalQuestions", exam.getTotalQuestions());
                
                var testSessions = testSessionRepository.findAll().stream()
                        .filter(ts -> ts.getExam().getId().equals(examId))
                        .collect(Collectors.toList());
                
                stats.put("totalAttempts", testSessions.size());
                
                long completed = testSessions.stream()
                        .filter(ts -> ts.getStatus() == TestSessionStatus.COMPLETED)
                        .count();
                stats.put("completedAttempts", completed);
                
                List<BigDecimal> scores = testSessions.stream()
                        .filter(ts -> ts.getStatus() == TestSessionStatus.COMPLETED)
                        .map(ts -> ts.getTotalScore())
                        .filter(score -> score != null)
                        .collect(Collectors.toList());
                
                if (!scores.isEmpty()) {
                BigDecimal average = scores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
                stats.put("averageScore", average);
                
                BigDecimal max = scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal min = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                
                stats.put("maxScore", max);
                stats.put("minScore", min);
                
                long passed = scores.stream()
                        .count();
                stats.put("passedCount", passed);
                stats.put("passRate", BigDecimal.valueOf(passed)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)));
                } else {
                stats.put("averageScore", 0.0);
                stats.put("maxScore", 0.0);
                stats.put("minScore", 0.0);
                stats.put("passedCount", 0);
                stats.put("passRate", 0.0);
                }
                
                return stats;
        }

        @Transactional(readOnly = true)
        public List<Map<String, Object>> getRecentTestSessions(int limit) {
                return testSessionRepository.findAll().stream()
                        .sorted((ts1, ts2) -> ts2.getStartedAt().compareTo(ts1.getStartedAt()))
                        .limit(limit)
                        .map(ts -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ts.getId());
                        map.put("studentName", ts.getStudentName());
                        map.put("examName", ts.getExam().getName());
                        map.put("status", ts.getStatus().name());
                        map.put("totalScore", ts.getTotalScore());
                        map.put("startedAt", ts.getStartedAt());
                        map.put("completedAt", ts.getCompletedAt());
                        return map;
                        })
                        .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public Map<String, Object> getStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
                Map<String, Object> stats = new HashMap<>();
                
                long questionsCreated = questionRepository.findAll().stream()
                        .filter(q -> q.getCreatedAt().isAfter(startDate) 
                                && q.getCreatedAt().isBefore(endDate))
                        .count();
                stats.put("questionsCreated", questionsCreated);
                
                long examsCreated = examRepository.findAll().stream()
                        .filter(e -> e.getCreatedAt().isAfter(startDate) 
                                && e.getCreatedAt().isBefore(endDate))
                        .count();
                stats.put("examsCreated", examsCreated);
                
                long testsTaken = testSessionRepository.findAll().stream()
                        .filter(ts -> ts.getStartedAt().isAfter(startDate) 
                                && ts.getStartedAt().isBefore(endDate))
                        .count();
                stats.put("testsTaken", testsTaken);
                
                long testsCompleted = testSessionRepository.findAll().stream()
                        .filter(ts -> ts.getStatus() == TestSessionStatus.COMPLETED
                                && ts.getCompletedAt() != null
                                && ts.getCompletedAt().isAfter(startDate) 
                                && ts.getCompletedAt().isBefore(endDate))
                        .count();
                stats.put("testsCompleted", testsCompleted);
                
                return stats;
        }
}
