package com.aispeaking.repository;

import com.aispeaking.entity.TestSession;
import com.aispeaking.entity.enums.TestSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    
    Page<TestSession> findByExamId(Long examId, Pageable pageable);
    
    Page<TestSession> findByStatus(TestSessionStatus status, Pageable pageable);
    
    @Query("SELECT t FROM TestSession t WHERE 1=1 " +
            "AND (:examId IS NULL OR t.exam.id = :examId) " +
            "AND (:studentName IS NULL OR LOWER(t.studentName) LIKE LOWER(CONCAT('%', :studentName, '%'))) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:minScore IS NULL OR t.totalScore >= :minScore) " +
            "AND (:maxScore IS NULL OR t.totalScore <= :maxScore) " +
            "AND (:fromDate IS NULL OR t.startedAt >= :fromDate) " +
            "AND (:toDate IS NULL OR t.startedAt <= :toDate)")
    Page<TestSession> findByCriteria(
        @Param("examId") Long examId,
        @Param("studentName") String studentName,
        @Param("status") TestSessionStatus status,
        @Param("minScore") BigDecimal minScore,
        @Param("maxScore") BigDecimal maxScore,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );
}
