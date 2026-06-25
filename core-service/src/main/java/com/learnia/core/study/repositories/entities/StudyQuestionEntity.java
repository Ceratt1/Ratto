package com.learnia.core.study.repositories.entities;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_question")
public class StudyQuestionEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "problem_set_id", nullable = false)
    private StudyProblemSetEntity problemSet;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String theme;

    @Column(nullable = false)
    private String difficulty;

    @Column(name = "general_explanation", nullable = false, columnDefinition = "TEXT")
    private String generalExplanation;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<StudyAnswerEntity> answers = new ArrayList<>();

    protected StudyQuestionEntity() {
    }

    public StudyQuestionEntity(int position, String question, String subject, String theme, String difficulty,
            String generalExplanation) {
        this.position = position;
        this.question = question;
        this.subject = subject;
        this.theme = theme;
        this.difficulty = difficulty;
        this.generalExplanation = generalExplanation;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    void attachTo(StudyProblemSetEntity problemSet) {
        this.problemSet = problemSet;
    }

    public void addAnswer(StudyAnswerEntity answer) {
        answer.attachTo(this);
        answers.add(answer);
    }

    public UUID getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getQuestion() {
        return question;
    }

    public String getSubject() {
        return subject;
    }

    public String getTheme() {
        return theme;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getGeneralExplanation() {
        return generalExplanation;
    }

    public List<StudyAnswerEntity> getAnswers() {
        return answers;
    }
}
