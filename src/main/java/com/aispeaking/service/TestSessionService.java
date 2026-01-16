package com.aispeaking.service;

import com.aispeaking.dto.*;
import com.aispeaking.entity.*;
import com.aispeaking.entity.enums.ProcessingStatus;
import com.aispeaking.entity.enums.TestSessionStatus;
import com.aispeaking.repository.TestAnswerRepository;
import com.aispeaking.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestSessionService {

    private final TestSessionRepository testSessionRepository;
    private final TestAnswerRepository testAnswerRepository;
    private final ExamService examService;
    private final QuestionService questionService;
    private final AIProcessingService aiProcessingService;

    @Transactional(readOnly = true)
    public Page<TestSessionResponse> getAllTestSessions(Pageable pageable) {
        return testSessionRepository.findAll(pageable)
                .map(TestSessionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TestSessionResponse> searchTestSessions(
            Long examId,
            String studentName,
            TestSessionStatus status,
            BigDecimal minScore,
            BigDecimal maxScore,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        return testSessionRepository.findByCriteria(
                examId, studentName, status, minScore, maxScore, fromDate, toDate, pageable)
                .map(TestSessionResponse::from);
    }

    @Transactional
    public TestSessionResponse createTestSession(Long examId, String studentName, String studentOrganization) {
        Exam exam = examService.getExamEntityById(examId);
        
        TestSession testSession = new TestSession();
        testSession.setExam(exam);
        testSession.setStudentName(studentName);
        testSession.setStudentOrganization(studentOrganization);
        testSession.setStartedAt(LocalDateTime.now());
        testSession.setStatus(TestSessionStatus.IN_PROGRESS);
        
        TestSession savedSession = testSessionRepository.save(testSession);
        
        // Create test answers for all exam questions
        List<ExamQuestionResponse> examQuestions = examService.getExamQuestions(examId);
        for (ExamQuestionResponse examQuestionResponse : examQuestions) {
            TestAnswer testAnswer = new TestAnswer();
            testAnswer.setTestSession(savedSession);
            // Get Question entity from database
            Question question = questionService.getQuestionEntityById(examQuestionResponse.getQuestionId());
            testAnswer.setQuestion(question);
            testAnswer.setProcessingStatus(ProcessingStatus.PENDING);
            testAnswerRepository.save(testAnswer);
        }
        
        log.info("Created test session {} for student {}", savedSession.getId(), studentName);
        return TestSessionResponse.from(savedSession);
    }

    @Transactional
    public TestAnswerResponse submitAnswer(Long testSessionId, Long questionId, MultipartFile audioFile) throws IOException {
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
        TestAnswer savedAnswer = testAnswerRepository.save(testAnswer);
        
        // Process with AI (async)
        aiProcessingService.processTestAnswer(savedAnswer);
        
        return TestAnswerResponse.from(savedAnswer);
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
    public TestSessionResponse getTestSession(Long id) {
        TestSession testSession = testSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test session not found"));
        return TestSessionResponse.from(testSession);
    }
    
    // Internal use only - for other services that need TestSession entity
    @Transactional(readOnly = true)
    public TestSession getTestSessionEntityById(Long id) {
        return testSessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test session not found"));
    }

    @Transactional(readOnly = true)
    public List<TestAnswerResponse> getTestAnswers(Long testSessionId) {
        return testAnswerRepository.findByTestSessionId(testSessionId).stream()
                .map(TestAnswerResponse::from)
                .collect(Collectors.toList());
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
