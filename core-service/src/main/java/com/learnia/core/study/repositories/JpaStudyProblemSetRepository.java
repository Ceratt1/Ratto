package com.learnia.core.study.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.learnia.core.study.repositories.entities.StudyProblemSetEntity;

public interface JpaStudyProblemSetRepository extends JpaRepository<StudyProblemSetEntity, UUID> {

    boolean existsByFileUuid(UUID fileUuid);

    Optional<StudyProblemSetEntity> findByFileUuid(UUID fileUuid);

    List<StudyProblemSetEntity> findByUserIdAndWorkspace_IdOrderByCreatedAtDesc(UUID userId, UUID workspaceId);

    List<StudyProblemSetEntity> findByUserIdAndWorkspaceIsNullOrderByCreatedAtDesc(UUID userId);

    List<StudyProblemSetEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("select p from StudyProblemSetEntity p where p.id = :id and p.userId = :userId")
    Optional<StudyProblemSetEntity> findWithQuestionsByIdAndUserId(UUID id, UUID userId);

    Optional<StudyProblemSetEntity> findByIdAndUserId(UUID id, UUID userId);
}
