package com.aispeaking.controller;

import com.aispeaking.dto.*;
import com.aispeaking.entity.enums.QuestionLevel;
import com.aispeaking.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class QuestionController {

    private final QuestionService questionService;

    /**
     * Get all questions with pagination
     * GET /questions?page=0&size=10&sort=id,desc
     * 
     * Response JSON (Page):
     * {
     *   "content": [
     *     {
     *       "id": 1,
     *       "content": "Describe your hometown",
     *       "level": "EASY",
     *       "createdByUsername": "admin",
     *       "createdAt": "2026-01-15T10:00:00"
     *     }
     *   ],
     *   "totalElements": 100,
     *   "totalPages": 10,
     *   "size": 10,
     *   "number": 0
     * }
     */
    @GetMapping
    public ResponseEntity<Page<QuestionResponse>> getAllQuestions(Pageable pageable) {
        return ResponseEntity.ok(questionService.getAllQuestions(pageable));
    }

    /**
     * Get question by ID
     * GET /questions/{id}
     * 
     * Response JSON:
     * {
     *   "id": 1,
     *   "content": "Describe your hometown",
     *   "level": "EASY",
     *   "createdByUsername": "admin",
     *   "createdAt": "2026-01-15T10:00:00",
     *   "updatedAt": "2026-01-15T10:00:00"
     * }
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionById(id));
    }

    /**
     * Search questions with filters
     * GET /questions/search?level=EASY&createdByUsername=admin
     * 
     * Response JSON: Same as getAllQuestions
     */
    @GetMapping("/search")
    public ResponseEntity<Page<QuestionResponse>> searchQuestions(
            @RequestParam(required = false) QuestionLevel level,
            @RequestParam(required = false) String createdByUsername,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        return ResponseEntity.ok(questionService.searchQuestions(level, createdByUsername, fromDate, toDate, pageable));
    }

    /**
     * Get random questions
     * GET /questions/random?level=EASY&count=10
     * 
     * Response JSON: Array of QuestionResponse (same structure as single question)
     */
    @GetMapping("/random")
    public ResponseEntity<List<QuestionResponse>> getRandomQuestions(
            @RequestParam(required = false) QuestionLevel level,
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(questionService.getRandomQuestions(level, count));
    }

    /**
     * Create new question
     * POST /questions
     *
     * Request JSON:
     * {
     *   "content": "Describe your hometown",
     *   "level": "EASY",
     *   "sampleAnswers": [
     *     {
     *       "content": "My hometown is a small city...",
     *       "score": 1
     *     },
     *     {
     *       "content": "My hometown is a beautiful coastal city...",
     *       "score": 5
     *     }
     *   ]
     * }
     *
     * Response JSON:
     * Same as getQuestionById
     */
    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.ok(questionService.createQuestion(request));
    }

    /**
     * Update question
     * PUT /questions/{id}
     * 
     * Request JSON:
     * {
     *   "content": "Updated question content",
     *   "level": "HARD"
     * }
     * 
     * Response JSON: Same as getQuestionById
     */
    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return ResponseEntity.ok(questionService.updateQuestion(id, request));
    }

    /**
     * Delete question
     * DELETE /questions/{id}
     * 
     * Response: 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
    
    // ============= Sample Answer Endpoints =============
    
    /**
     * Get all sample answers for a question
     * GET /questions/{questionId}/sample-answers
     * 
     * Response JSON:
     * [
     *   {
     *     "id": 1,
     *     "questionId": 5,
     *     "content": "My hometown is a small city...",
     *     "score": 1,
     *     "createdAt": "2026-01-15T10:00:00"
     *   }
     * ]
     */
    @GetMapping("/{questionId}/sample-answers")
    public ResponseEntity<List<SampleAnswerResponse>> getSampleAnswers(@PathVariable Long questionId) {
        return ResponseEntity.ok(questionService.getSampleAnswers(questionId));
    }
    
    /**
     * Get a specific sample answer
     * GET /questions/{questionId}/sample-answers/{sampleAnswerId}
     * 
     * Response JSON: Same as item in getSampleAnswers
     */
    @GetMapping("/{questionId}/sample-answers/{sampleAnswerId}")
    public ResponseEntity<SampleAnswerResponse> getSampleAnswerById(
            @PathVariable Long questionId,
            @PathVariable Long sampleAnswerId) {
        return ResponseEntity.ok(questionService.getSampleAnswerById(questionId, sampleAnswerId));
    }
    
    /**
     * Create a new sample answer for a question
     * POST /questions/{questionId}/sample-answers
     * 
     * Request JSON:
     * {
     *   "content": "My hometown is a beautiful coastal city...",
     *   "score": 5
     * }
     * 
     * Response JSON: Same as getSampleAnswerById
     */
    @PostMapping("/{questionId}/sample-answers")
    public ResponseEntity<SampleAnswerResponse> createSampleAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody CreateSampleAnswerRequest request) {
        return ResponseEntity.ok(questionService.createSampleAnswer(questionId, request));
    }
    
    /**
     * Update a sample answer
     * PUT /questions/{questionId}/sample-answers/{sampleAnswerId}
     * 
     * Request JSON:
     * {
     *   "content": "Updated content...",
     *   "score": 8
     * }
     * 
     * Response JSON: Same as getSampleAnswerById
     */
    @PutMapping("/{questionId}/sample-answers/{sampleAnswerId}")
    public ResponseEntity<SampleAnswerResponse> updateSampleAnswer(
            @PathVariable Long questionId,
            @PathVariable Long sampleAnswerId,
            @Valid @RequestBody UpdateSampleAnswerRequest request) {
        return ResponseEntity.ok(questionService.updateSampleAnswer(questionId, sampleAnswerId, request));
    }
    
    /**
     * Delete a sample answer
     * DELETE /questions/{questionId}/sample-answers/{sampleAnswerId}
     * 
     * Response: 204 No Content
     */
    @DeleteMapping("/{questionId}/sample-answers/{sampleAnswerId}")
    public ResponseEntity<Void> deleteSampleAnswer(
            @PathVariable Long questionId,
            @PathVariable Long sampleAnswerId) {
        questionService.deleteSampleAnswer(questionId, sampleAnswerId);
        return ResponseEntity.noContent().build();
    }
}
