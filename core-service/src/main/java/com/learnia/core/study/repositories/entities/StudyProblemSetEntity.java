package com.learnia.core.study.repositories.entities;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_problem_set")
public class StudyProblemSetEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "file_uuid", nullable = false, unique = true)
    private UUID fileUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private StudyWorkspaceEntity workspace;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(length = 200)
    private String description;

    @Column(name = "document_language", nullable = false)
    private String documentLanguage;

    @Column(name = "study_language", nullable = false)
    private String studyLanguage;

    @Column(name = "document_summary", nullable = false, columnDefinition = "TEXT")
    private String documentSummary;

    @Column(name = "ai_provider", nullable = false)
    private String aiProvider;

    @Column(name = "ai_model", nullable = false)
    private String aiModel;

    @Column(name = "extracted_text_s3_path", nullable = false, length = 1024)
    private String extractedTextS3Path;

    @Column(name = "study_problems_s3_path", nullable = false, length = 1024)
    private String studyProblemsS3Path;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "problemSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<StudyQuestionEntity> questions = new ArrayList<>();

    protected StudyProblemSetEntity() {
    }

    public StudyProblemSetEntity(
            UUID userId,
            UUID fileUuid,
            StudyWorkspaceEntity workspace,
            String originalFileName,
            String description,
            String documentLanguage,
            String studyLanguage,
            String documentSummary,
            String aiProvider,
            String aiModel,
            String extractedTextS3Path,
            String studyProblemsS3Path) {
        this.userId = userId;
        this.fileUuid = fileUuid;
        this.workspace = workspace;
        this.originalFileName = originalFileName;
        this.description = description;
        this.documentLanguage = documentLanguage;
        this.studyLanguage = studyLanguage;
        this.documentSummary = documentSummary;
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
        this.extractedTextS3Path = extractedTextS3Path;
        this.studyProblemsS3Path = studyProblemsS3Path;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void addQuestion(StudyQuestionEntity question) {
        question.attachTo(this);
        questions.add(question);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getFileUuid() {
        return fileUuid;
    }

    public StudyWorkspaceEntity getWorkspace() {
        return workspace;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getDescription() {
        return description;
    }

    public String getDocumentLanguage() {
        return documentLanguage;
    }

    public String getStudyLanguage() {
        return studyLanguage;
    }

    public String getDocumentSummary() {
        return documentSummary;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<StudyQuestionEntity> getQuestions() {
        return questions;
    }

    public void moveTo(StudyWorkspaceEntity workspace) {
        this.workspace = workspace;
    }
}
