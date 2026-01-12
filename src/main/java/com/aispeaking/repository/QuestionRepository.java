package com.aispeaking.repository;

import com.aispeaking.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByType(Question.QuestionType type);
    List<Question> findByDifficulty(Question.DifficultyLevel difficulty);
    List<Question> findByTopic(String topic);
    List<Question> findByActiveTrue();
}
