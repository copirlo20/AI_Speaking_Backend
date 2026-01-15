package com.aispeaking.service;

import com.aispeaking.dto.*;
import com.aispeaking.entity.Question;
import com.aispeaking.entity.enums.QuestionLevel;
import com.aispeaking.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public Page<QuestionResponse> getAllQuestions(Pageable pageable) {
        return questionRepository.findByDeletedAtIsNull(pageable)
                .map(QuestionResponse::from);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .filter(q -> q.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        return QuestionResponse.from(question);
    }
    
    @Transactional(readOnly = true)
    public Question getQuestionEntityById(Long id) {
        return questionRepository.findById(id)
                .filter(q -> q.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> searchQuestions(
            QuestionLevel level,
            String createdByUsername,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        return questionRepository.findByCriteria(level, createdByUsername, fromDate, toDate, pageable)
                .map(QuestionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getRandomQuestions(QuestionLevel level, int count) {
        return questionRepository.findRandomQuestions(level, Pageable.ofSize(count))
                .stream()
                .map(QuestionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        Question question = new Question();
        question.setContent(request.getContent());
        question.setLevel(request.getLevel());
        
        Question savedQuestion = questionRepository.save(question);
        log.info("Creating new question: {}", question.getContent());
        return QuestionResponse.from(savedQuestion);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long id, UpdateQuestionRequest request) {
        Question question = getQuestionEntityById(id);
        
        if (request.getContent() != null) {
            question.setContent(request.getContent());
        }
        if (request.getLevel() != null) {
            question.setLevel(request.getLevel());
        }
        
        Question savedQuestion = questionRepository.save(question);
        log.info("Updated question with id: {}", id);
        return QuestionResponse.from(savedQuestion);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        Question question = getQuestionEntityById(id);
        question.softDelete();
        questionRepository.save(question);
        log.info("Soft deleted question with id: {}", id);
    }
}
