package com.learnia.core.study.repositories.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_attempt")
public class StudyAttemptEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "problem_set_id", nullable = false)
    private StudyProblemSetEntity problemSet;

    @Column(nullable = false)
    private String status;

    private BigDecimal score;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyAttemptAnswerEntity> answers = new ArrayList<>();

    protected StudyAttemptEntity() {
    }

    public StudyAttemptEntity(UUID userId, StudyProblemSetEntity problemSet) {
        this.userId = userId;
        this.problemSet = problemSet;
        this.status = "IN_PROGRESS";
        this.totalQuestions = problemSet.getQuestions().size();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
    }

    public void submit(List<StudyAttemptAnswerEntity> answers, int correctCount) {
        this.answers.clear();
        answers.forEach(answer -> answer.attachTo(this));
        this.answers.addAll(answers);
        updateScore(correctCount);
        this.status = "SUBMITTED";
        this.submittedAt = OffsetDateTime.now();
    }

    public StudyAttemptAnswerEntity answer(StudyQuestionEntity question, StudyAnswerEntity selectedAnswer) {
        answers.removeIf(answer -> answer.getQuestion().getId().equals(question.getId()));
        StudyAttemptAnswerEntity answer = new StudyAttemptAnswerEntity(question, selectedAnswer);
        answer.attachTo(this);
        answers.add(answer);
        updateScore((int) answers.stream().filter(StudyAttemptAnswerEntity::isCorrect).count());
        if (answers.size() == totalQuestions) {
            this.status = "SUBMITTED";
            this.submittedAt = OffsetDateTime.now();
        }
        return answer;
    }

    private void updateScore(int correctCount) {
        this.correctCount = correctCount;
        this.score = totalQuestions == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctCount * 10000L / totalQuestions, 2);
    }

    public UUID getId() {
        return id;
    }

    public StudyProblemSetEntity getProblemSet() {
        return problemSet;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public List<StudyAttemptAnswerEntity> getAnswers() {
        return answers;
    }
}
