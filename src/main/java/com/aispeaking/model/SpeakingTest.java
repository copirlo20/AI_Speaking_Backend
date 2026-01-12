package com.aispeaking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "speaking_tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeakingTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private TestLevel level;

    @Column(columnDefinition = "int default 0")
    private Integer duration = 0; // in minutes

    @Column(columnDefinition = "int default 0")
    private Integer totalQuestions = 0;

    @ManyToMany
    @JoinTable(
        name = "test_questions",
        joinColumns = @JoinColumn(name = "test_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private Set<Question> questions = new HashSet<>();

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TestResult> testResults = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private boolean active = true;

    @Column(columnDefinition = "int default 0")
    private Integer attemptCount = 0;

    public enum TestLevel {
        A1,
        A2,
        B1,
        B2,
        C1,
        C2
    }
}
