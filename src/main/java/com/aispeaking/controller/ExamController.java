package com.aispeaking.controller;

import com.aispeaking.entity.Exam;
import com.aispeaking.entity.ExamQuestion;
import com.aispeaking.entity.enums.QuestionLevel;
import com.aispeaking.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class ExamController {

    private final ExamService examService;

    @GetMapping
    public ResponseEntity<Page<Exam>> getAllExams(Pageable pageable) {
        return ResponseEntity.ok(examService.getAllExams(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Exam> getExamById(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamById(id));
    }

    @PostMapping
    public ResponseEntity<Exam> createExam(@RequestBody Exam exam) {
        return ResponseEntity.ok(examService.createExam(exam));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Exam> updateExam(
            @PathVariable Long id,
            @RequestBody Exam exam) {
        return ResponseEntity.ok(examService.updateExam(id, exam));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<Void> addQuestionsToExam(
            @PathVariable Long id,
            @RequestBody List<Long> questionIds) {
        examService.addQuestionsToExam(id, questionIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/generate-random")
    public ResponseEntity<Void> generateRandomExam(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        QuestionLevel level = params.containsKey("level") 
                ? QuestionLevel.valueOf(params.get("level").toString()) 
                : null;
        int count = Integer.parseInt(params.get("count").toString());
        
        examService.generateRandomExam(id, level, count);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<ExamQuestion>> getExamQuestions(@PathVariable Long id) {
        return ResponseEntity.ok(examService.getExamQuestions(id));
    }
}
