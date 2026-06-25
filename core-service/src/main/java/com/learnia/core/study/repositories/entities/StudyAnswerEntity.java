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
@Table(name = "study_answer")
public class StudyAnswerEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private StudyQuestionEntity question;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    protected StudyAnswerEntity() {
    }

    public StudyAnswerEntity(int position, String answer, boolean correct, String explanation) {
        this.position = position;
        this.answer = answer;
        this.correct = correct;
        this.explanation = explanation;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    void attachTo(StudyQuestionEntity question) {
        this.question = question;
    }

    public UUID getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isCorrect() {
        return correct;
    }

    public String getExplanation() {
        return explanation;
    }
}
