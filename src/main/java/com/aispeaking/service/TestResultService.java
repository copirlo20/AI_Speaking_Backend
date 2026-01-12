package com.aispeaking.service;

import com.aispeaking.model.TestResult;
import com.aispeaking.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TestResultService {

    private final TestResultRepository testResultRepository;
    private final UserService userService;

    @Transactional
    public TestResult createTestResult(TestResult testResult) {
        // Calculate overall score
        double overall = (
            testResult.getPronunciationScore() +
            testResult.getFluencyScore() +
            testResult.getGrammarScore() +
            testResult.getVocabularyScore() +
            testResult.getContentScore()
        ) / 5.0;
        
        testResult.setOverallScore(overall);
        TestResult savedResult = testResultRepository.save(testResult);
        
        // Update user statistics
        updateUserStatistics(testResult.getUser().getId());
        
        return savedResult;
    }

    public Optional<TestResult> getTestResultById(Long id) {
        return testResultRepository.findById(id);
    }

    public List<TestResult> getAllTestResults() {
        return testResultRepository.findAll();
    }

    public List<TestResult> getTestResultsByUserId(Long userId) {
        return testResultRepository.findByUserId(userId);
    }

    public List<TestResult> getTestResultsByTestId(Long testId) {
        return testResultRepository.findByTestId(testId);
    }

    public List<TestResult> getTestResultsByUserAndTest(Long userId, Long testId) {
        return testResultRepository.findByUserIdAndTestId(userId, testId);
    }

    @Transactional
    public TestResult updateTestResult(Long id, TestResult resultDetails) {
        TestResult result = testResultRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Test result not found"));
        
        result.setTranscription(resultDetails.getTranscription());
        result.setPronunciationScore(resultDetails.getPronunciationScore());
        result.setFluencyScore(resultDetails.getFluencyScore());
        result.setGrammarScore(resultDetails.getGrammarScore());
        result.setVocabularyScore(resultDetails.getVocabularyScore());
        result.setContentScore(resultDetails.getContentScore());
        result.setAiFeedback(resultDetails.getAiFeedback());
        result.setSuggestions(resultDetails.getSuggestions());
        result.setStatus(resultDetails.getStatus());
        
        // Recalculate overall score
        double overall = (
            result.getPronunciationScore() +
            result.getFluencyScore() +
            result.getGrammarScore() +
            result.getVocabularyScore() +
            result.getContentScore()
        ) / 5.0;
        result.setOverallScore(overall);
        
        return testResultRepository.save(result);
    }

    @Transactional
    public void deleteTestResult(Long id) {
        testResultRepository.deleteById(id);
    }

    private void updateUserStatistics(Long userId) {
        Double averageScore = testResultRepository.findAverageScoreByUserId(userId);
        List<TestResult> userResults = testResultRepository.findByUserId(userId);
        userService.updateUserStats(userId, averageScore != null ? averageScore : 0.0, userResults.size());
    }
}
