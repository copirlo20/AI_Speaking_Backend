package com.aispeaking.repository;

import com.aispeaking.entity.AIProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AIProcessingLogRepository extends JpaRepository<AIProcessingLog, Long> {
    
    List<AIProcessingLog> findByTestAnswerId(Long testAnswerId);
}
