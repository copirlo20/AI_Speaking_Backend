package com.aispeaking.controller;

import com.aispeaking.dto.*;
import com.aispeaking.entity.enums.ExamStatus;
import com.aispeaking.service.ExamService;
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
@RequestMapping("/exams")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class ExamController {
    private final ExamService examService;

    /**
     * Lấy tất cả đề thi với phân trang
     * GET /exams?page=0&size=10&sort=id,desc
     * 
     * Response JSON (Page):
     * {
     *   "content": [
     *     {
     *       "id": 1,
     *       "name": "English Speaking Test Level 1",
     *       "description": "Basic speaking test",
     *       "status": "ACTIVE",
     *       "totalQuestions": 10,
     *       "createdByUsername": "johndoe",
     *       "createdAt": "2026-01-15T10:00:00"
     *     }
     *   ],
     *   "totalElements": 20,
     *   "totalPages": 2,
     *   "size": 10,
     *   "number": 0
     * }
     */
    @GetMapping
    public ResponseEntity<Page<ExamResponse>> getAllExams(Pageable pageable) {
        return ResponseEntity.ok(examService.getAllExams(pageable));
    }

    /**
     * Tìm kiếm đề thi với các tiêu chí khác nhau
     * GET /exams/search?status=ACTIVE&createdByUsername=johndoe&fromDate=2026-01-01T00:00:00
     * 
     * Response JSON: Same as getAllExams
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ExamResponse>> searchExams(
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(required = false) String createdByUsername,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        return ResponseEntity.ok(examService.searchExams(status, createdByUsername, fromDate, toDate, pageable));
    }

    /**
     * Lấy đề thi theo ID
     * GET /exams/{id}
     * 
     * Response JSON:
     * {
     *   "id": 1,
     *   "name": "English Speaking Test Level 1",
     *   "description": "Basic speaking test for beginners",
     *   "status": "ACTIVE",
     *   "totalQuestions": 10,
     *   "createdByUsername": "johndoe",
     *   "createdAt": "2026-01-15T10:00:00",
     *   "updatedAt": "2026-01-15T10:00:00"
     * }
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExamResponse> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamById(id));
    }

    /**
     * Tạo đề thi mới
     * POST /exams
     * 
     * Request JSON:
     * {
     *   "name": "English Speaking Test Level 1",
     *   "description": "Basic speaking test for beginners",
     *   "status": "DRAFT"
     * }
     * 
     * Response JSON: Same as getExamById
     */
    @PostMapping
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody CreateExamRequest request) {
        return ResponseEntity.ok(examService.createExam(request));
    }

    /**
     * Cập nhật đề thi
     * PUT /exams/{id}
     * 
     * Request JSON:
     * {
     *   "name": "Updated Exam Name",
     *   "description": "Updated description",
     *   "status": "ACTIVE"
     * }
     * 
     * Response JSON: Same as getExamById
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamRequest request) {
        return ResponseEntity.ok(examService.updateExam(id, request));
    }

    /**
     * Xóa đề thi
     * DELETE /exams/{id}
     * 
     * Response: 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Thêm câu hỏi vào đề thi
     * POST /exams/{id}/questions
     * 
     * Request JSON:
     * {
     *   "questionIds": [1, 2, 3, 4, 5]
     * }
     * 
     * Response: 200 OK (empty body)
     */
    @PostMapping("/{id}/questions")
    public ResponseEntity<Void> addQuestionsToExam(
            @PathVariable Long id,
            @Valid @RequestBody AddQuestionsToExamRequest request) {
        examService.addQuestionsToExam(id, request.getQuestionIds());
        return ResponseEntity.ok().build();
    }

    /**
     * Tự động tạo đề thi với câu hỏi ngẫu nhiên
     * POST /exams/{id}/generate-random
     * 
     * Request JSON:
     * {
     *   "level": "EASY",  // EASY or HARD
     *   "count": 10
     * }
     * 
     * Response: 200 OK (empty body)
     */
    @PostMapping("/{id}/generate-random")
    public ResponseEntity<Void> generateRandomExam(
            @PathVariable Long id,
            @Valid @RequestBody GenerateRandomExamRequest request) {
        examService.generateRandomExam(id, request.getLevel(), request.getCount());
        return ResponseEntity.ok().build();
    }

    /**
     * Lấy tất cả câu hỏi trong đề thi
     * GET /exams/{id}/questions
     * 
     * Response JSON:
     * [
     *   {
     *     "id": 1,
     *     "questionId": 5,
     *     "questionContent": "Describe your hometown",
     *     "questionLevel": "EASY",
     *     "orderNumber": 1
     *   },
     *   {
     *     "id": 2,
     *     "questionId": 8,
     *     "questionContent": "What are your career goals?",
     *     "questionLevel": "HARD",
     *     "orderNumber": 2
     *   }
     * ]
     */
    @GetMapping("/{id}/questions")
    public ResponseEntity<List<ExamQuestionResponse>> getExamQuestions(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamQuestions(id));
    }
}