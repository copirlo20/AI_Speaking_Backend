package com.aispeaking.repository;

import com.aispeaking.entity.Exam;
import com.aispeaking.entity.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    
    Page<Exam> findByDeletedAtIsNull(Pageable pageable);
    
    Page<Exam> findByStatusAndDeletedAtIsNull(ExamStatus status, Pageable pageable);
    
    @Query("SELECT e FROM Exam e WHERE e.deletedAt IS NULL " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:createdBy IS NULL OR e.createdBy.id = :createdBy) " +
           "AND (:fromDate IS NULL OR e.createdAt >= :fromDate) " +
           "AND (:toDate IS NULL OR e.createdAt <= :toDate)")
    Page<Exam> findByCriteria(
        @Param("status") ExamStatus status,
        @Param("createdBy") Long createdBy,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );
}
