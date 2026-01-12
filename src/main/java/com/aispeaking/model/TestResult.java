package com.aispeaking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private SpeakingTest test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(length = 5000)
    private String transcription;

    private String audioFilePath;

    @Column(columnDefinition = "double default 0.0")
    private Double pronunciationScore = 0.0;

    @Column(columnDefinition = "double default 0.0")
    private Double fluencyScore = 0.0;

    @Column(columnDefinition = "double default 0.0")
    private Double grammarScore = 0.0;

    @Column(columnDefinition = "double default 0.0")
    private Double vocabularyScore = 0.0;

    @Column(columnDefinition = "double default 0.0")
    private Double contentScore = 0.0;

    @Column(columnDefinition = "double default 0.0")
    private Double overallScore = 0.0;

    @Column(length = 3000)
    private String aiFeedback;

    @Column(length = 2000)
    private String suggestions;

    @Column(columnDefinition = "int default 0")
    private Integer recordingDuration = 0; // in seconds

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    public enum AnalysisStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
