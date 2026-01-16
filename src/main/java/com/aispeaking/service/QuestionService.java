package com.aispeaking.service;

import com.aispeaking.dto.*;
import com.aispeaking.entity.Question;
import com.aispeaking.entity.SampleAnswer;
import com.aispeaking.entity.User;
import com.aispeaking.entity.enums.QuestionLevel;
import com.aispeaking.repository.QuestionRepository;
import com.aispeaking.repository.SampleAnswerRepository;
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
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SampleAnswerRepository sampleAnswerRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<QuestionResponse> getAllQuestions(Pageable pageable) {
        return questionRepository.findAll(pageable)
            .map(QuestionResponse::from);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        return QuestionResponse.from(question);
    }
    
    @Transactional(readOnly = true)
    public Question getQuestionEntityById(Long id) {
        return questionRepository.findById(id)
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
        
        // Set createdBy from current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            User createdBy = userService.getUserEntityByUsername(authentication.getName());
            question.setCreatedBy(createdBy);
        }
        
        Question savedQuestion = questionRepository.save(question);
        
        // Create sample answers if provided
        if (request.getSampleAnswers() != null && !request.getSampleAnswers().isEmpty()) {
            for (CreateQuestionRequest.SampleAnswerDto sampleDto : request.getSampleAnswers()) {
                SampleAnswer sampleAnswer = new SampleAnswer();
                sampleAnswer.setQuestion(savedQuestion);
                sampleAnswer.setContent(sampleDto.getContent());
                sampleAnswer.setScore(sampleDto.getScore());
                sampleAnswerRepository.save(sampleAnswer);
            }
            log.info("Created {} sample answers for question {}", request.getSampleAnswers().size(), savedQuestion.getId());
        }
        
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
        questionRepository.deleteById(id);
        log.info("Hard deleted question with id: {}", id);
    }
    
    // ============= Sample Answer CRUD Methods =============
    
    @Transactional(readOnly = true)
    public List<SampleAnswerResponse> getSampleAnswers(Long questionId) {
        return sampleAnswerRepository.findByQuestionId(questionId).stream()
                .map(SampleAnswerResponse::from)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public SampleAnswerResponse getSampleAnswerById(Long questionId, Long sampleAnswerId) {
        SampleAnswer sampleAnswer = sampleAnswerRepository.findById(sampleAnswerId)
                .orElseThrow(() -> new RuntimeException("Sample answer not found with id: " + sampleAnswerId));
        
        if (!sampleAnswer.getQuestion().getId().equals(questionId)) {
            throw new RuntimeException("Sample answer does not belong to question " + questionId);
        }
        
        return SampleAnswerResponse.from(sampleAnswer);
    }
    
    @Transactional
    public SampleAnswerResponse createSampleAnswer(Long questionId, CreateSampleAnswerRequest request) {
        Question question = getQuestionEntityById(questionId);
        
        SampleAnswer sampleAnswer = new SampleAnswer();
        sampleAnswer.setQuestion(question);
        sampleAnswer.setContent(request.getContent());
        sampleAnswer.setScore(request.getScore());
        
        SampleAnswer saved = sampleAnswerRepository.save(sampleAnswer);
        log.info("Created sample answer for question {}", questionId);
        return SampleAnswerResponse.from(saved);
    }
    
    @Transactional
    public SampleAnswerResponse updateSampleAnswer(Long questionId, Long sampleAnswerId, UpdateSampleAnswerRequest request) {
        SampleAnswer sampleAnswer = sampleAnswerRepository.findById(sampleAnswerId)
                .orElseThrow(() -> new RuntimeException("Sample answer not found with id: " + sampleAnswerId));
        
        if (!sampleAnswer.getQuestion().getId().equals(questionId)) {
            throw new RuntimeException("Sample answer does not belong to question " + questionId);
        }
        
        if (request.getContent() != null) {
            sampleAnswer.setContent(request.getContent());
        }
        if (request.getScore() != null) {
            sampleAnswer.setScore(request.getScore());
        }
        
        SampleAnswer saved = sampleAnswerRepository.save(sampleAnswer);
        log.info("Updated sample answer {} for question {}", sampleAnswerId, questionId);
        return SampleAnswerResponse.from(saved);
    }
    
    @Transactional
    public void deleteSampleAnswer(Long questionId, Long sampleAnswerId) {
        SampleAnswer sampleAnswer = sampleAnswerRepository.findById(sampleAnswerId)
                .orElseThrow(() -> new RuntimeException("Sample answer not found with id: " + sampleAnswerId));
        
        if (!sampleAnswer.getQuestion().getId().equals(questionId)) {
            throw new RuntimeException("Sample answer does not belong to question " + questionId);
        }
        
        sampleAnswerRepository.deleteById(sampleAnswerId);
        log.info("Deleted sample answer {} from question {}", sampleAnswerId, questionId);
    }
}
