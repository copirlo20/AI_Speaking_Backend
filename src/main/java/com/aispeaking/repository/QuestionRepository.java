package com.aispeaking.repository;

import com.aispeaking.entity.Question;
import com.aispeaking.entity.enums.QuestionLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    Page<Question> findAll(Pageable pageable);
    
    Page<Question> findByLevel(QuestionLevel level, Pageable pageable);
    
    @Query("""
        SELECT q FROM Question q
        WHERE (:level IS NULL OR q.level = :level)
        AND (:createdByUsername IS NULL OR q.createdBy.username = :createdByUsername)
        AND (:fromDate IS NULL OR q.createdAt >= :fromDate)
        AND (:toDate IS NULL OR q.createdAt <= :toDate)
    """)
    Page<Question> findByCriteria(
        @Param("level") QuestionLevel level,
        @Param("createdByUsername") String createdByUsername,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );
    
    @Query("""
        SELECT q FROM Question q
        WHERE (:level IS NULL OR q.level = :level)
        ORDER BY RAND()
    """)
    List<Question> findRandomQuestions(
        @Param("level") QuestionLevel level,
        Pageable pageable
    );
}
