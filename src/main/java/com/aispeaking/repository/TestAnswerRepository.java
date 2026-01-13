package com.aispeaking.repository;

import com.aispeaking.entity.TestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestAnswerRepository extends JpaRepository<TestAnswer, Long> {
    
    List<TestAnswer> findByTestSessionId(Long testSessionId);
}
