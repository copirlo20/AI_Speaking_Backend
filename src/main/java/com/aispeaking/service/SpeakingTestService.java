package com.aispeaking.service;

import com.aispeaking.model.SpeakingTest;
import com.aispeaking.repository.SpeakingTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SpeakingTestService {

    private final SpeakingTestRepository testRepository;

    @Transactional
    public SpeakingTest createTest(SpeakingTest test) {
        test.setTotalQuestions(test.getQuestions().size());
        return testRepository.save(test);
    }

    public Optional<SpeakingTest> getTestById(Long id) {
        return testRepository.findById(id);
    }

    public List<SpeakingTest> getAllTests() {
        return testRepository.findAll();
    }

    public List<SpeakingTest> getActiveTests() {
        return testRepository.findByActiveTrue();
    }

    public List<SpeakingTest> getTestsByLevel(SpeakingTest.TestLevel level) {
        return testRepository.findByLevel(level);
    }

    @Transactional
    public SpeakingTest updateTest(Long id, SpeakingTest testDetails) {
        SpeakingTest test = testRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        test.setTitle(testDetails.getTitle());
        test.setDescription(testDetails.getDescription());
        test.setLevel(testDetails.getLevel());
        test.setDuration(testDetails.getDuration());
        test.setActive(testDetails.isActive());
        
        if (testDetails.getQuestions() != null) {
            test.setQuestions(testDetails.getQuestions());
            test.setTotalQuestions(testDetails.getQuestions().size());
        }
        
        return testRepository.save(test);
    }

    @Transactional
    public void deleteTest(Long id) {
        testRepository.deleteById(id);
    }

    @Transactional
    public void incrementAttemptCount(Long testId) {
        SpeakingTest test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        test.setAttemptCount(test.getAttemptCount() + 1);
        testRepository.save(test);
    }
}
