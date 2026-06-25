package com.learnia.core.study.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.learnia.core.study.repositories.entities.StudyWorkspaceEntity;

public interface JpaStudyWorkspaceRepository extends JpaRepository<StudyWorkspaceEntity, UUID> {

    List<StudyWorkspaceEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<StudyWorkspaceEntity> findByIdAndUserId(UUID id, UUID userId);
}
