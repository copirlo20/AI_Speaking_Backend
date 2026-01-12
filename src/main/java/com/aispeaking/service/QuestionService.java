package com.aispeaking.service;

import com.aispeaking.model.Question;
import com.aispeaking.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Transactional
    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public List<Question> getActiveQuestions() {
        return questionRepository.findByActiveTrue();
    }

    public List<Question> getQuestionsByType(Question.QuestionType type) {
        return questionRepository.findByType(type);
    }

    public List<Question> getQuestionsByDifficulty(Question.DifficultyLevel difficulty) {
        return questionRepository.findByDifficulty(difficulty);
    }

    public List<Question> getQuestionsByTopic(String topic) {
        return questionRepository.findByTopic(topic);
    }

    @Transactional
    public Question updateQuestion(Long id, Question questionDetails) {
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Question not found"));
        
        question.setQuestionText(questionDetails.getQuestionText());
        question.setSampleAnswer(questionDetails.getSampleAnswer());
        question.setType(questionDetails.getType());
        question.setDifficulty(questionDetails.getDifficulty());
        question.setTopic(questionDetails.getTopic());
        question.setEvaluationCriteria(questionDetails.getEvaluationCriteria());
        question.setTimeLimit(questionDetails.getTimeLimit());
        question.setPreparationTime(questionDetails.getPreparationTime());
        question.setActive(questionDetails.isActive());
        
        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }
}
