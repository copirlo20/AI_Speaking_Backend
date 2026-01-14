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

    @GetMapping
    public ResponseEntity<Page<ExamResponse>> getAllExams(Pageable pageable) {
        return ResponseEntity.ok(examService.getAllExams(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ExamResponse>> searchExams(
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Pageable pageable) {
        return ResponseEntity.ok(examService.searchExams(status, createdBy, fromDate, toDate, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamResponse> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamById(id));
    }

    @PostMapping
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody CreateExamRequest request) {
        return ResponseEntity.ok(examService.createExam(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamRequest request) {
        return ResponseEntity.ok(examService.updateExam(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<Void> addQuestionsToExam(
            @PathVariable Long id,
            @Valid @RequestBody AddQuestionsToExamRequest request) {
        examService.addQuestionsToExam(id, request.getQuestionIds());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/generate-random")
    public ResponseEntity<Void> generateRandomExam(
            @PathVariable Long id,
            @Valid @RequestBody GenerateRandomExamRequest request) {
        examService.generateRandomExam(id, request.getLevel(), request.getCount());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<ExamQuestionResponse>> getExamQuestions(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamQuestions(id));
    }
}
