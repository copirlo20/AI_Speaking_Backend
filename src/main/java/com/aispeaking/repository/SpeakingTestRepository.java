package com.aispeaking.repository;

import com.aispeaking.model.SpeakingTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeakingTestRepository extends JpaRepository<SpeakingTest, Long> {
    List<SpeakingTest> findByLevel(SpeakingTest.TestLevel level);
    List<SpeakingTest> findByActiveTrue();
}
