package com.aispeaking.repository;

import com.aispeaking.entity.Exam;
import com.aispeaking.entity.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    
    Page<Exam> findByDeletedAtIsNull(Pageable pageable);
    
    Page<Exam> findByStatusAndDeletedAtIsNull(ExamStatus status, Pageable pageable);
}
