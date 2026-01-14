package com.aispeaking.service;

import com.aispeaking.entity.*;
import com.aispeaking.entity.enums.ProcessingStatus;
import com.aispeaking.entity.enums.TestSessionStatus;
import com.aispeaking.repository.TestAnswerRepository;
import com.aispeaking.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestSessionService {

    private final TestSessionRepository testSessionRepository;
    private final TestAnswerRepository testAnswerRepository;
    private final ExamService examService;
    private final AIProcessingService aiProcessingService;

    @Transactional
    public TestSession createTestSession(Long examId, String studentName, String studentOrganization, String studentEmail) {
        Exam exam = examService.getExamById(examId);
        
        TestSession testSession = new TestSession();
        testSession.setExam(exam);
        testSession.setStudentName(studentName);
        testSession.setStudentOrganization(studentOrganization);
        testSession.setStudentEmail(studentEmail);
        testSession.setStartedAt(LocalDateTime.now());
        testSession.setStatus(TestSessionStatus.IN_PROGRESS);
        
        TestSession savedSession = testSessionRepository.save(testSession);
        
        // Create test answers for all exam questions
        List<ExamQuestion> examQuestions = examService.getExamQuestions(examId);
        for (ExamQuestion examQuestion : examQuestions) {
            TestAnswer testAnswer = new TestAnswer();
            testAnswer.setTestSession(savedSession);
            testAnswer.setQuestion(examQuestion.getQuestion());
            testAnswer.setProcessingStatus(ProcessingStatus.PENDING);
            testAnswerRepository.save(testAnswer);
        }
        
        log.info("Created test session {} for student {}", savedSession.getId(), studentName);
        return savedSession;
    }

    @Transactional
    public TestAnswer submitAnswer(Long testSessionId, Long questionId, MultipartFile audioFile) throws IOException {
        // Find the test answer
        TestAnswer testAnswer = testAnswerRepository.findByTestSessionId(testSessionId).stream()
                .filter(ta -> ta.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test answer not found"));
        
        // Save audio file
        String audioUrl = saveAudioFile(audioFile, testSessionId, questionId);
        testAnswer.setAudioUrl(audioUrl);
        testAnswer.setAnsweredAt(LocalDateTime.now());
        testAnswer.setProcessingStatus(ProcessingStatus.TRANSCRIBING);
        testAnswerRepository.save(testAnswer);
        
        // Process with AI (async)
        aiProcessingService.processTestAnswer(testAnswer);
        
        return testAnswer;
    }

    @Transactional
    public void completeTestSession(Long testSessionId) {
        TestSession testSession = testSessionRepository.findById(testSessionId)
                .orElseThrow(() -> new RuntimeException("Test session not found"));
        
        // Calculate average score from all answers
        BigDecimal averageScore = calculateAverageScore(testSessionId);
        
        testSession.setTotalScore(averageScore);
        testSession.setCompletedAt(LocalDateTime.now());
        testSession.setStatus(TestSessionStatus.COMPLETED);
        testSessionRepository.save(testSession);
        
        log.info("Completed test session {} with average score {}", testSessionId, averageScore);
    }

    /**
     * Calculate average score for a test session
     * Formula: Average score = Sum of all answer scores / Total number of answers
     * Score scale: 0-10
     * 
     * @param testSessionId The test session ID
     * @return Average score rounded to 2 decimal places
     */
    private BigDecimal calculateAverageScore(Long testSessionId) {
        List<TestAnswer> testAnswers = testAnswerRepository.findByTestSessionId(testSessionId);
        
        if (testAnswers.isEmpty()) {
            log.warn("No answers found for test session {}", testSessionId);
            return BigDecimal.ZERO;
        }
        
        // Sum all answer scores
        BigDecimal totalScore = testAnswers.stream()
                .map(TestAnswer::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate average (rounded to 2 decimal places)
        BigDecimal averageScore = totalScore.divide(
                BigDecimal.valueOf(testAnswers.size()),
                2,
                RoundingMode.HALF_UP
        );
        
        log.debug("Test session {}: {} answers, total score = {}, average = {}", 
                testSessionId, testAnswers.size(), totalScore, averageScore);
        
        return averageScore;
    }

    @Transactional(readOnly = true)
    public TestSession getTestSession(Long id) {
        return testSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test session not found"));
    }

    @Transactional(readOnly = true)
    public List<TestAnswer> getTestAnswers(Long testSessionId) {
        return testAnswerRepository.findByTestSessionId(testSessionId);
    }

    private String saveAudioFile(MultipartFile file, Long testSessionId, Long questionId) throws IOException {
        String uploadDir = "uploads/audio/" + testSessionId;
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String filename = questionId + "_" + UUID.randomUUID() + ".wav";
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        
        return uploadDir + "/" + filename;
    }
}
