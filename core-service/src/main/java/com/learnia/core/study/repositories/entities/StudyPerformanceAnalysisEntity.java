package com.learnia.core.study.repositories.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.PerformanceAnalysisResult;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "study_performance_analysis")
public class StudyPerformanceAnalysisEntity {

    @Id
    private UUID id;

    @Column(name = "analysis_request_id", nullable = false, unique = true)
    private UUID analysisRequestId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_set_id", nullable = false)
    private StudyProblemSetEntity problemSet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private StudyAttemptEntity attempt;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String markdown;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String strengths = "[]";

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String gaps = "[]";

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String evolution = "[]";

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String recommendations = "[]";

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String exercises = "[]";

    @Column(name = "analysis_references", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String references = "[]";

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "ai_provider")
    private String aiProvider;

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StudyPerformanceAnalysisEntity() {
    }

    public StudyPerformanceAnalysisEntity(
            UUID analysisRequestId,
            UUID userId,
            StudyProblemSetEntity problemSet,
            StudyAttemptEntity attempt,
            int version) {
        this.analysisRequestId = analysisRequestId;
        this.userId = userId;
        this.problemSet = problemSet;
        this.attempt = attempt;
        this.version = version;
        this.status = "PENDING";
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        requestedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void markReady(
            String aiProvider,
            String aiModel,
            PerformanceAnalysisResult result,
            JsonFields jsonFields) {
        if ("READY".equals(status)) {
            return;
        }
        this.status = "READY";
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
        this.summary = result.summary();
        this.markdown = result.markdown();
        this.strengths = jsonFields.strengths();
        this.gaps = jsonFields.gaps();
        this.evolution = jsonFields.evolution();
        this.recommendations = jsonFields.recommendations();
        this.exercises = jsonFields.exercises();
        this.references = jsonFields.references();
        this.failureReason = null;
        this.generatedAt = OffsetDateTime.now();
    }

    public void markFailed(String reason) {
        if ("READY".equals(status)) {
            return;
        }
        this.status = "FAILED";
        this.failureReason = reason;
    }

    public UUID getAnalysisRequestId() {
        return analysisRequestId;
    }

    public String getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public String getMarkdown() {
        return markdown;
    }

    public String getStrengths() {
        return strengths;
    }

    public String getGaps() {
        return gaps;
    }

    public String getEvolution() {
        return evolution;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public String getExercises() {
        return exercises;
    }

    public String getReferences() {
        return references;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public int getVersion() {
        return version;
    }

    public record JsonFields(
            String strengths,
            String gaps,
            String evolution,
            String recommendations,
            String exercises,
            String references) {
    }
}
