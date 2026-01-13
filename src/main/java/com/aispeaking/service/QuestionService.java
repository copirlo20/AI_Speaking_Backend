package com.aispeaking.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public Page<Question> getAllQuestions(Pageable pageable) {
        return questionRepository.findByDeletedAtIsNull(pageable);
    }

    @Transactional(readOnly = true)
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .filter(q -> q.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Question> searchQuestions(
            QuestionLevel level,
            String category,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        return questionRepository.findByCriteria(level, category, fromDate, toDate, pageable);
    }

    @Transactional(readOnly = true)
    public List<Question> getRandomQuestions(QuestionLevel level, int count) {
        return questionRepository.findRandomQuestions(
                level,
                Pageable.ofSize(count)
        );
    }

    @Transactional
    public Question createQuestion(Question question) {
        log.info("Creating new question: {}", question.getContent());
        return questionRepository.save(question);
    }

    @Transactional
    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = getQuestionById(id);
        question.setContent(questionDetails.getContent());
        question.setLevel(questionDetails.getLevel());
        question.setCategory(questionDetails.getCategory());
        log.info("Updated question with id: {}", id);
        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        Question question = getQuestionById(id);
        question.softDelete();
        questionRepository.save(question);
        log.info("Soft deleted question with id: {}", id);
    }
}
