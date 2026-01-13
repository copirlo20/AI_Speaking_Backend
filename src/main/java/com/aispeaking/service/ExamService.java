package com.aispeaking.service;

import com.aispeaking.entity.Exam;
import com.aispeaking.entity.ExamQuestion;
import com.aispeaking.entity.Question;
import com.aispeaking.entity.enums.QuestionLevel;
import com.aispeaking.repository.ExamQuestionRepository;
import com.aispeaking.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionService questionService;

    @Transactional(readOnly = true)
    public Page<Exam> getAllExams(Pageable pageable) {
        return examRepository.findByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public Exam getExamById(Long id) {
        return examRepository.findById(id)
                .filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + id));
    }

    @Transactional
    public Exam createExam(Exam exam) {
        log.info("Creating new exam: {}", exam.getName());
        return examRepository.save(exam);
    }

    @Transactional
    public Exam updateExam(Long id, Exam examDetails) {
        Exam exam = getExamById(id);
        exam.setName(examDetails.getName());
        exam.setDescription(examDetails.getDescription());
        exam.setDurationMinutes(examDetails.getDurationMinutes());
        exam.setTotalQuestions(examDetails.getTotalQuestions());
        exam.setStatus(examDetails.getStatus());
        log.info("Updated exam with id: {}", id);
        return examRepository.save(exam);
    }

    @Transactional
    public void deleteExam(Long id) {
        Exam exam = getExamById(id);
        exam.softDelete();
        examRepository.save(exam);
        log.info("Soft deleted exam with id: {}", id);
    }

    @Transactional
    public void addQuestionsToExam(Long examId, List<Long> questionIds) {
        Exam exam = getExamById(examId);
        
        int order = 1;
        for (Long questionId : questionIds) {
            Question question = questionService.getQuestionById(questionId);
            
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
        Exam exam = getExamById(examId);
        
        // Clear existing questions
        examQuestionRepository.deleteByExamId(examId);
        
        // Get random questions
        List<Question> randomQuestions = questionService.getRandomQuestions(level, count);
        
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
    public List<ExamQuestion> getExamQuestions(Long examId) {
        return examQuestionRepository.findByExamIdOrderByQuestionOrder(examId);
    }
}
