package com.aispeaking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String questionText;

    @Column(length = 2000)
    private String sampleAnswer;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty;

    private String topic;

    @Column(length = 2000)
    private String evaluationCriteria;

    @Column(columnDefinition = "int default 0")
    private Integer timeLimit = 60; // seconds

    @Column(columnDefinition = "int default 0")
    private Integer preparationTime = 30; // seconds

    private String imageUrl;

    private String audioUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private boolean active = true;

    public enum QuestionType {
        INTRODUCTION,
        DESCRIPTION,
        OPINION,
        STORYTELLING,
        SITUATION_RESPONSE,
        PICTURE_DESCRIPTION,
        TOPIC_DISCUSSION
    }

    public enum DifficultyLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }
}
