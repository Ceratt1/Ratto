package com.learnia.core.study.repositories.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_attempt_answer")
public class StudyAttemptAnswerEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private StudyAttemptEntity attempt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private StudyQuestionEntity question;

    @ManyToOne(optional = false)
    @JoinColumn(name = "selected_answer_id", nullable = false)
    private StudyAnswerEntity selectedAnswer;

    @Column(nullable = false)
    private boolean correct;

    protected StudyAttemptAnswerEntity() {
    }

    public StudyAttemptAnswerEntity(StudyQuestionEntity question, StudyAnswerEntity selectedAnswer) {
        this.question = question;
        this.selectedAnswer = selectedAnswer;
        this.correct = selectedAnswer.isCorrect();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    void attachTo(StudyAttemptEntity attempt) {
        this.attempt = attempt;
    }

    public StudyQuestionEntity getQuestion() {
        return question;
    }

    public StudyAnswerEntity getSelectedAnswer() {
        return selectedAnswer;
    }

    public boolean isCorrect() {
        return correct;
    }
}
