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
        return testSessionRepository.findAll(pageable).map(TestSessionResponse::from);
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
        return testSessionRepository.findByCriteria(examId, studentName, status, minScore, maxScore, fromDate, toDate, pageable).map(TestSessionResponse::from);
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
        // Tạo các câu trả lời kiểm tra cho tất cả các câu hỏi trong đề thi
        List<ExamQuestionResponse> examQuestions = examService.getExamQuestions(examId);
        for (ExamQuestionResponse examQuestionResponse : examQuestions) {
            TestAnswer testAnswer = new TestAnswer();
            testAnswer.setTestSession(savedSession);
            // Lấy thực thể Question từ cơ sở dữ liệu
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
        log.info("Starting submit answer for test session {} question {}", testSessionId, questionId);
        // Tìm câu trả lời kiểm tra
        TestAnswer testAnswer = testAnswerRepository.findByTestSessionId(testSessionId).stream()
                .filter(ta -> ta.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test answer not found"));
        // Lưu file âm thanh trước
        String audioUrl = saveAudioFile(audioFile, testSessionId, questionId);
        testAnswer.setAudioUrl(audioUrl);
        testAnswer.setAnsweredAt(LocalDateTime.now());
        // Xử lý với AI đồng bộ (chặn cho đến khi Whisper và Qwen hoàn thành)
        // Điều này sẽ cập nhật tất cả các trường: transcribedText, score, feedback, processingStatus
        aiProcessingService.processTestAnswerSync(testAnswer);
        // Lưu vào DB chỉ MỘT LẦN sau khi tất cả quá trình xử lý hoàn tất
        TestAnswer savedAnswer = testAnswerRepository.save(testAnswer);
        log.info("Completed submit answer for test session {} question {}, status: {}, score: {}", testSessionId, questionId, savedAnswer.getProcessingStatus(), savedAnswer.getScore());
        return TestAnswerResponse.from(savedAnswer);
    }

    @Transactional
    public void completeTestSession(Long testSessionId) {
        TestSession testSession = testSessionRepository.findById(testSessionId).orElseThrow(() -> new RuntimeException("Test session not found"));
        // Tính điểm trung bình từ tất cả các câu trả lời
        BigDecimal averageScore = calculateAverageScore(testSessionId);
        testSession.setTotalScore(averageScore);
        testSession.setCompletedAt(LocalDateTime.now());
        testSession.setStatus(TestSessionStatus.COMPLETED);
        testSessionRepository.save(testSession);
        log.info("Completed test session {} with average score {}", testSessionId, averageScore);
    }

    /**
     * Tính điểm trung bình cho một phiên kiểm tra
     * Công thức: Điểm trung bình = Tổng điểm tất cả các câu trả lời / Tổng số câu trả lời
     * Thang điểm: 0-10
     * 
     * param testSessionId ID của phiên kiểm tra
     * return Điểm trung bình làm tròn 2 chữ số thập phân
     */
    private BigDecimal calculateAverageScore(Long testSessionId) {
        List<TestAnswer> testAnswers = testAnswerRepository.findByTestSessionId(testSessionId);
        if (testAnswers.isEmpty()) {
            log.warn("No answers found for test session {}", testSessionId);
            return BigDecimal.ZERO;
        }
        // Tính tổng điểm tất cả các câu trả lời
        BigDecimal totalScore = testAnswers.stream().map(TestAnswer::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Tính điểm trung bình (làm tròn 2 chữ số thập phân)
        BigDecimal averageScore = totalScore.divide(
                BigDecimal.valueOf(testAnswers.size()),
                2,
                RoundingMode.HALF_UP
        );
        log.debug("Test session {}: {} answers, total score = {}, average = {}", testSessionId, testAnswers.size(), totalScore, averageScore);
        return averageScore;
    }

    @Transactional(readOnly = true)
    public TestSessionResponse getTestSession(Long id) {
        TestSession testSession = testSessionRepository.findById(id).orElseThrow(() -> new RuntimeException("Test session not found"));
        return TestSessionResponse.from(testSession);
    }
    
    // Chỉ sử dụng nội bộ - cho các service khác cần thực thể TestSession
    @Transactional(readOnly = true)
    public TestSession getTestSessionEntityById(Long id) {
        return testSessionRepository.findById(id).orElseThrow(() -> new RuntimeException("Test session not found"));
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