package com.learnia.core.study.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.learnia.core.study.repositories.entities.StudyAttemptEntity;

public interface JpaStudyAttemptRepository extends JpaRepository<StudyAttemptEntity, UUID> {

    @Query("select a from StudyAttemptEntity a where a.id = :id and a.userId = :userId")
    Optional<StudyAttemptEntity> findWithDetailsByIdAndUserId(UUID id, UUID userId);
}
