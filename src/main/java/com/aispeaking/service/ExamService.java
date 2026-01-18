package com.aispeaking.service;

import com.aispeaking.dto.*;
import com.aispeaking.entity.Exam;
import com.aispeaking.entity.ExamQuestion;
import com.aispeaking.entity.Question;
import com.aispeaking.entity.User;
import com.aispeaking.entity.enums.ExamStatus;
import com.aispeaking.entity.enums.QuestionLevel;
import com.aispeaking.repository.ExamQuestionRepository;
import com.aispeaking.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamService {
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionService questionService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<ExamResponse> getAllExams(Pageable pageable) {
        return examRepository.findAll(pageable).map(ExamResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ExamResponse> searchExams(
            ExamStatus status,
            String createdByUsername,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        return examRepository.findByCriteria(status, createdByUsername, fromDate, toDate, pageable).map(ExamResponse::from);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExamById(Long id) {
        Exam exam = examRepository.findById(id).orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));
        return ExamResponse.from(exam);
    }
    
    @Transactional(readOnly = true)
    public Exam getExamEntityById(Long id) {
        return examRepository.findById(id).orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));
    }

    @Transactional
    public ExamResponse createExam(CreateExamRequest request) {
        Exam exam = new Exam();
        exam.setName(request.getName());
        exam.setDescription(request.getDescription());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setTotalQuestions(request.getTotalQuestions());
        exam.setStatus(request.getStatus() != null ? request.getStatus() : ExamStatus.DRAFT);   
        // Đặt createdBy từ người dùng đã xác thực hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            User createdBy = userService.getUserEntityByUsername(authentication.getName());
            exam.setCreatedBy(createdBy);
        }
        Exam savedExam = examRepository.save(exam);
        log.info("Creating new exam: {}", exam.getName());
        return ExamResponse.from(savedExam);
    }

    @Transactional
    public ExamResponse updateExam(Long id, UpdateExamRequest request) {
        Exam exam = getExamEntityById(id);
        if (request.getName() != null) {
            exam.setName(request.getName());
        }
        if (request.getDescription() != null) {
            exam.setDescription(request.getDescription());
        }
        if (request.getDurationMinutes() != null) {
            exam.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getTotalQuestions() != null) {
            exam.setTotalQuestions(request.getTotalQuestions());
        }
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
        }
        Exam savedExam = examRepository.save(exam);
        log.info("Updated exam with id: {}", id);
        return ExamResponse.from(savedExam);
    }

    @Transactional
    public void deleteExam(Long id) {
        examRepository.deleteById(id);
        log.info("Deleted exam with id: {}", id);
    }

    @Transactional
    public void addQuestionsToExam(Long examId, List<Long> questionIds) {
        Exam exam = getExamEntityById(examId);
        int order = 1;
        for (Long questionId : questionIds) {
            Question question = questionService.getQuestionEntityById(questionId);
            ExamQuestion examQuestion = new ExamQuestion();
            examQuestion.setExam(exam);
            examQuestion.setQuestion(question);
            examQuestion.setQuestionOrder(order++);
            examQuestionRepository.save(examQuestion);
        }
        exam.setTotalQuestions(questionIds.size());
        examRepository.save(exam);
        log.info("Added {} questions to exam {}", questionIds.size(), examId);
    }

    @Transactional
    public void generateRandomExam(Long examId, QuestionLevel level, int count) {
        Exam exam = getExamEntityById(examId);
        // Xóa các câu hỏi hiện tại
        examQuestionRepository.deleteByExamId(examId);
        // Lấy các câu hỏi ngẫu nhiên - cần entity không phải DTO
        List<Question> randomQuestions = questionService.getRandomQuestions(level, count)
                .stream()
                .map(dto -> questionService.getQuestionEntityById(dto.getId()))
                .collect(Collectors.toList());
        int order = 1;
        for (Question question : randomQuestions) {
            ExamQuestion examQuestion = new ExamQuestion();
            examQuestion.setExam(exam);
            examQuestion.setQuestion(question);
            examQuestion.setQuestionOrder(order++);
            examQuestionRepository.save(examQuestion);
        }
        exam.setTotalQuestions(randomQuestions.size());
        examRepository.save(exam);
        log.info("Generated random exam {} with {} questions", examId, randomQuestions.size());
    }

    @Transactional(readOnly = true)
    public List<ExamQuestionResponse> getExamQuestions(Long examId) {
        return examQuestionRepository.findByExamIdOrderByQuestionOrder(examId)
                .stream()
                .map(ExamQuestionResponse::from)
                .collect(Collectors.toList());
    }
}