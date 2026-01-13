package com.aispeaking.repository;

import com.aispeaking.entity.TestSession;
import com.aispeaking.entity.enums.TestSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    
    Page<TestSession> findByExamId(Long examId, Pageable pageable);
    
    Page<TestSession> findByStatus(TestSessionStatus status, Pageable pageable);
}
