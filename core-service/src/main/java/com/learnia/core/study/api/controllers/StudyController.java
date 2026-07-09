package com.learnia.core.study.api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.learnia.core.identity.services.AuthenticatedUserService;
import com.learnia.core.study.api.dtos.StudyDtos.AnswerAttemptQuestionRequest;
import com.learnia.core.study.api.dtos.StudyDtos.AnswerAttemptQuestionResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AttemptResponse;
import com.learnia.core.study.api.dtos.StudyDtos.MoveProblemSetRequest;
import com.learnia.core.study.api.dtos.StudyDtos.PerformanceAnalysisResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetDetailResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetPerformanceResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetSummaryResponse;
import com.learnia.core.study.api.dtos.StudyDtos.SubmitAttemptRequest;
import com.learnia.core.study.api.dtos.StudyDtos.WorkspacePerformanceResponse;
import com.learnia.core.study.api.dtos.WorkspaceDtos.WorkspaceRequest;
import com.learnia.core.study.api.dtos.WorkspaceDtos.WorkspaceResponse;
import com.learnia.core.study.services.StudyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/study")
public class StudyController {

    private final AuthenticatedUserService authenticatedUserService;
    private final StudyService studyService;

    public StudyController(AuthenticatedUserService authenticatedUserService, StudyService studyService) {
        this.authenticatedUserService = authenticatedUserService;
        this.studyService = studyService;
    }

    @GetMapping("/workspaces")
    public List<WorkspaceResponse> listWorkspaces(@AuthenticationPrincipal Jwt jwt) {
        return studyService.listWorkspaces(userId(jwt));
    }

    @PostMapping("/workspaces")
    public WorkspaceResponse createWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WorkspaceRequest request) {
        return studyService.createWorkspace(userId(jwt), request);
    }

    @PatchMapping("/workspaces/{workspaceId}")
    public WorkspaceResponse updateWorkspace(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceRequest request) {
        return studyService.updateWorkspace(userId(jwt), workspaceId, request);
    }

    @GetMapping("/problem-sets")
    public List<ProblemSetSummaryResponse> listProblemSets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(defaultValue = "false") boolean unassigned) {
        return studyService.listProblemSets(userId(jwt), workspaceId, unassigned);
    }

    @PatchMapping("/problem-sets/{problemSetId}/workspace")
    public ProblemSetSummaryResponse moveProblemSet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID problemSetId,
            @RequestBody MoveProblemSetRequest request) {
        return studyService.moveProblemSet(userId(jwt), problemSetId, request);
    }

    @GetMapping("/problem-sets/{problemSetId}")
    public ProblemSetDetailResponse getProblemSet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID problemSetId) {
        return studyService.getProblemSet(userId(jwt), problemSetId);
    }

    @GetMapping("/performance")
    public WorkspacePerformanceResponse getWorkspacePerformance(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID workspaceId) {
        return studyService.getWorkspacePerformance(userId(jwt), workspaceId);
    }

    @GetMapping("/problem-sets/{problemSetId}/performance")
    public ProblemSetPerformanceResponse getProblemSetPerformance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID problemSetId) {
        return studyService.getProblemSetPerformance(userId(jwt), problemSetId);
    }

    @GetMapping("/problem-sets/{problemSetId}/performance-analysis")
    public PerformanceAnalysisResponse getLatestPerformanceAnalysis(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID problemSetId) {
        return studyService.getLatestPerformanceAnalysis(userId(jwt), problemSetId);
    }

    @PostMapping("/problem-sets/{problemSetId}/performance-analysis/retry")
    public PerformanceAnalysisResponse retryPerformanceAnalysis(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID problemSetId) {
        return studyService.retryPerformanceAnalysis(userId(jwt), problemSetId);
    }

    @PostMapping("/problem-sets/{problemSetId}/attempts")
    public AttemptResponse startAttempt(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID problemSetId) {
        return studyService.startAttempt(userId(jwt), problemSetId);
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptResponse getAttempt(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID attemptId) {
        return studyService.getAttempt(userId(jwt), attemptId);
    }

    @PostMapping("/attempts/{attemptId}/answers")
    public AnswerAttemptQuestionResponse answerAttemptQuestion(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID attemptId,
            @Valid @RequestBody AnswerAttemptQuestionRequest request) {
        return studyService.answerAttemptQuestion(userId(jwt), attemptId, request);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public AttemptResponse submitAttempt(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID attemptId,
            @Valid @RequestBody SubmitAttemptRequest request) {
        return studyService.submitAttempt(userId(jwt), attemptId, request);
    }

    private UUID userId(Jwt jwt) {
        return authenticatedUserService.from(jwt).id();
    }
}
