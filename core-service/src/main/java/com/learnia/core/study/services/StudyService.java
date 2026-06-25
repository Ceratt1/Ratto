package com.learnia.core.study.services;

import java.util.List;
import java.util.UUID;

import com.learnia.core.study.api.dtos.StudyDtos.AnswerAttemptQuestionRequest;
import com.learnia.core.study.api.dtos.StudyDtos.AnswerAttemptQuestionResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AttemptResponse;
import com.learnia.core.study.api.dtos.StudyDtos.MoveProblemSetRequest;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetDetailResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetSummaryResponse;
import com.learnia.core.study.api.dtos.StudyDtos.SubmitAttemptRequest;
import com.learnia.core.study.api.dtos.WorkspaceDtos.WorkspaceRequest;
import com.learnia.core.study.api.dtos.WorkspaceDtos.WorkspaceResponse;
import com.learnia.events.StudyProblemsGeneratedEvent;

public interface StudyService {

    List<WorkspaceResponse> listWorkspaces(UUID userId);

    WorkspaceResponse createWorkspace(UUID userId, WorkspaceRequest request);

    WorkspaceResponse updateWorkspace(UUID userId, UUID workspaceId, WorkspaceRequest request);

    List<ProblemSetSummaryResponse> listProblemSets(UUID userId, UUID workspaceId, boolean unassigned);

    ProblemSetSummaryResponse moveProblemSet(UUID userId, UUID problemSetId, MoveProblemSetRequest request);

    ProblemSetDetailResponse getProblemSet(UUID userId, UUID problemSetId);

    AttemptResponse startAttempt(UUID userId, UUID problemSetId);

    AttemptResponse getAttempt(UUID userId, UUID attemptId);

    AnswerAttemptQuestionResponse answerAttemptQuestion(UUID userId, UUID attemptId, AnswerAttemptQuestionRequest request);

    AttemptResponse submitAttempt(UUID userId, UUID attemptId, SubmitAttemptRequest request);

    void projectGeneratedProblems(StudyProblemsGeneratedEvent event);
}
