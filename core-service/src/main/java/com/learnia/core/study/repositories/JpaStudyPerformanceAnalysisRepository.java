package com.learnia.core.study.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.learnia.core.study.repositories.entities.StudyPerformanceAnalysisEntity;

public interface JpaStudyPerformanceAnalysisRepository extends JpaRepository<StudyPerformanceAnalysisEntity, UUID> {

    boolean existsByAttempt_Id(UUID attemptId);

    Optional<StudyPerformanceAnalysisEntity> findByAnalysisRequestId(UUID analysisRequestId);

    Optional<StudyPerformanceAnalysisEntity> findFirstByUserIdAndProblemSet_IdOrderByRequestedAtDesc(
            UUID userId,
            UUID problemSetId);

    Optional<StudyPerformanceAnalysisEntity> findFirstByUserIdAndProblemSet_IdAndAnalysisRequestIdNotOrderByRequestedAtDesc(
            UUID userId,
            UUID problemSetId,
            UUID analysisRequestId);

    @Query("select coalesce(max(analysis.version), 0) from StudyPerformanceAnalysisEntity analysis "
            + "where analysis.problemSet.id = :problemSetId")
    int latestVersion(UUID problemSetId);
}
