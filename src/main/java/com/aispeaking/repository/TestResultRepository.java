package com.aispeaking.repository;

import com.aispeaking.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findByUserId(Long userId);
    List<TestResult> findByTestId(Long testId);
    List<TestResult> findByUserIdAndTestId(Long userId, Long testId);
    
    @Query("SELECT AVG(tr.overallScore) FROM TestResult tr WHERE tr.user.id = :userId")
    Double findAverageScoreByUserId(Long userId);
}
