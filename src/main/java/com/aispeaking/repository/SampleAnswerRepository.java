package com.aispeaking.repository;

import com.aispeaking.entity.SampleAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SampleAnswerRepository extends JpaRepository<SampleAnswer, Long> {
    
    List<SampleAnswer> findByQuestionId(Long questionId);
}
