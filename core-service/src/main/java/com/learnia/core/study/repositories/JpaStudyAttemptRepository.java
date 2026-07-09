package com.learnia.core.study.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.learnia.core.study.repositories.entities.StudyAttemptEntity;

public interface JpaStudyAttemptRepository extends JpaRepository<StudyAttemptEntity, UUID> {

    @Query("select a from StudyAttemptEntity a where a.id = :id and a.userId = :userId")
    Optional<StudyAttemptEntity> findWithDetailsByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select distinct a from StudyAttemptEntity a
            join fetch a.problemSet p
            left join fetch a.answers answer
            left join fetch answer.question question
            left join fetch answer.selectedAnswer selectedAnswer
            where a.userId = :userId and p.id in :problemSetIds
            """)
    List<StudyAttemptEntity> findWithAnswersByUserIdAndProblemSetIds(UUID userId, Collection<UUID> problemSetIds);

    @Query("""
            select a from StudyAttemptEntity a
            join fetch a.problemSet p
            where a.userId = :userId and p.id = :problemSetId and a.status = 'SUBMITTED'
            order by a.submittedAt desc
            """)
    List<StudyAttemptEntity> findSubmittedByUserIdAndProblemSetIdOrderBySubmittedAtDesc(UUID userId, UUID problemSetId);
}
