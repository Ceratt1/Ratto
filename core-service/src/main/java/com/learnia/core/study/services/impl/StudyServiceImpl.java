package com.learnia.core.study.services.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learnia.core.study.api.dtos.StudyDtos.AnswerResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AnswerAttemptQuestionRequest;
import com.learnia.core.study.api.dtos.StudyDtos.AnswerAttemptQuestionResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AttemptAnswerResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AttemptResponse;
import com.learnia.core.study.api.dtos.StudyDtos.MoveProblemSetRequest;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetDetailResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetSummaryResponse;
import com.learnia.core.study.api.dtos.StudyDtos.QuestionResponse;
import com.learnia.core.study.api.dtos.StudyDtos.SubmitAttemptRequest;
import com.learnia.core.study.api.dtos.WorkspaceDtos.WorkspaceRequest;
import com.learnia.core.study.api.dtos.WorkspaceDtos.WorkspaceResponse;
import com.learnia.core.study.repositories.JpaStudyAttemptRepository;
import com.learnia.core.study.repositories.JpaStudyProblemSetRepository;
import com.learnia.core.study.repositories.JpaStudyWorkspaceRepository;
import com.learnia.core.study.repositories.entities.StudyAnswerEntity;
import com.learnia.core.study.repositories.entities.StudyAttemptAnswerEntity;
import com.learnia.core.study.repositories.entities.StudyAttemptEntity;
import com.learnia.core.study.repositories.entities.StudyProblemSetEntity;
import com.learnia.core.study.repositories.entities.StudyQuestionEntity;
import com.learnia.core.study.repositories.entities.StudyWorkspaceEntity;
import com.learnia.events.StudyProblemsGeneratedEvent;
import com.learnia.models.study.StudyProblemSet;
import com.learnia.tools.aws.service.S3StorageService;

import tools.jackson.databind.ObjectMapper;

@Service
public class StudyServiceImpl implements com.learnia.core.study.services.StudyService {

    private final JpaStudyWorkspaceRepository workspaceRepository;
    private final JpaStudyProblemSetRepository problemSetRepository;
    private final JpaStudyAttemptRepository attemptRepository;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;

    public StudyServiceImpl(
            JpaStudyWorkspaceRepository workspaceRepository,
            JpaStudyProblemSetRepository problemSetRepository,
            JpaStudyAttemptRepository attemptRepository,
            S3StorageService s3StorageService,
            ObjectMapper objectMapper) {
        this.workspaceRepository = workspaceRepository;
        this.problemSetRepository = problemSetRepository;
        this.attemptRepository = attemptRepository;
        this.s3StorageService = s3StorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces(UUID userId) {
        return workspaceRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toWorkspaceResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(UUID userId, WorkspaceRequest request) {
        return toWorkspaceResponse(workspaceRepository.save(new StudyWorkspaceEntity(
                userId,
                request.name().trim(),
                trimToNull(request.description()))));
    }

    @Override
    @Transactional
    public WorkspaceResponse updateWorkspace(UUID userId, UUID workspaceId, WorkspaceRequest request) {
        StudyWorkspaceEntity workspace = workspaceRepository.findByIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Área de estudo não encontrada."));
        workspace.update(request.name().trim(), trimToNull(request.description()));
        return toWorkspaceResponse(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProblemSetSummaryResponse> listProblemSets(UUID userId, UUID workspaceId, boolean unassigned) {
        List<StudyProblemSetEntity> problemSets;
        if (unassigned) {
            problemSets = problemSetRepository.findByUserIdAndWorkspaceIsNullOrderByCreatedAtDesc(userId);
        } else if (workspaceId != null) {
            problemSets = problemSetRepository.findByUserIdAndWorkspace_IdOrderByCreatedAtDesc(userId, workspaceId);
        } else {
            problemSets = problemSetRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return problemSets.stream().map(this::toProblemSetSummary).toList();
    }

    @Override
    @Transactional
    public ProblemSetSummaryResponse moveProblemSet(UUID userId, UUID problemSetId, MoveProblemSetRequest request) {
        StudyProblemSetEntity problemSet = problemSetRepository.findByIdAndUserId(problemSetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada."));
        StudyWorkspaceEntity workspace = request.workspaceId() == null
                ? null
                : workspaceRepository.findByIdAndUserId(request.workspaceId(), userId)
                        .orElseThrow(() -> new IllegalArgumentException("Área de estudo não encontrada."));
        problemSet.moveTo(workspace);
        return toProblemSetSummary(problemSet);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemSetDetailResponse getProblemSet(UUID userId, UUID problemSetId) {
        return toProblemSetDetail(problemSetRepository.findWithQuestionsByIdAndUserId(problemSetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada.")), false);
    }

    @Override
    @Transactional
    public AttemptResponse startAttempt(UUID userId, UUID problemSetId) {
        StudyProblemSetEntity problemSet = problemSetRepository.findWithQuestionsByIdAndUserId(problemSetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada."));
        return toAttemptResponse(attemptRepository.save(new StudyAttemptEntity(userId, problemSet)));
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResponse getAttempt(UUID userId, UUID attemptId) {
        return toAttemptResponse(attemptRepository.findWithDetailsByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tentativa não encontrada.")));
    }

    @Override
    @Transactional
    public AnswerAttemptQuestionResponse answerAttemptQuestion(
            UUID userId,
            UUID attemptId,
            AnswerAttemptQuestionRequest request) {
        StudyAttemptEntity attempt = attemptRepository.findWithDetailsByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tentativa não encontrada."));
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new IllegalArgumentException("Tentativa já finalizada.");
        }
        Map<UUID, StudyQuestionEntity> questions = attempt.getProblemSet().getQuestions().stream()
                .collect(Collectors.toMap(StudyQuestionEntity::getId, Function.identity()));
        StudyQuestionEntity question = require(questions, request.questionId(), "Questão inválida.");
        Map<UUID, StudyAnswerEntity> answers = question.getAnswers().stream()
                .collect(Collectors.toMap(StudyAnswerEntity::getId, Function.identity()));
        StudyAnswerEntity selected = require(answers, request.answerId(), "Alternativa inválida.");
        StudyAnswerEntity correctAnswer = question.getAnswers().stream()
                .filter(StudyAnswerEntity::isCorrect)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Questão sem alternativa correta."));
        StudyAttemptAnswerEntity recorded = attempt.answer(question, selected);
        int correctCount = attempt.getCorrectCount() == null ? 0 : attempt.getCorrectCount();
        return new AnswerAttemptQuestionResponse(
                question.getId(),
                selected.getId(),
                correctAnswer.getId(),
                recorded.isCorrect(),
                selected.getExplanation(),
                question.getGeneralExplanation(),
                attempt.getAnswers().size(),
                correctCount,
                attempt.getTotalQuestions(),
                attempt.getScore(),
                attempt.getStatus());
    }

    @Override
    @Transactional
    public AttemptResponse submitAttempt(UUID userId, UUID attemptId, SubmitAttemptRequest request) {
        StudyAttemptEntity attempt = attemptRepository.findWithDetailsByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Tentativa não encontrada."));
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new IllegalArgumentException("Tentativa já enviada.");
        }
        Map<UUID, StudyQuestionEntity> questions = attempt.getProblemSet().getQuestions().stream()
                .collect(Collectors.toMap(StudyQuestionEntity::getId, Function.identity()));
        Map<UUID, StudyAnswerEntity> answers = attempt.getProblemSet().getQuestions().stream()
                .flatMap(question -> question.getAnswers().stream())
                .collect(Collectors.toMap(StudyAnswerEntity::getId, Function.identity()));
        List<StudyAttemptAnswerEntity> attemptAnswers = request.answers().stream()
                .map(answer -> {
                    StudyQuestionEntity question = require(questions, answer.questionId(), "Questão inválida.");
                    StudyAnswerEntity selected = require(answers, answer.answerId(), "Alternativa inválida.");
                    if (question.getAnswers().stream().noneMatch(option -> option.getId().equals(selected.getId()))) {
                        throw new IllegalArgumentException("Alternativa não pertence à questão.");
                    }
                    return new StudyAttemptAnswerEntity(question, selected);
                })
                .toList();
        if (attemptAnswers.size() != questions.size()) {
            throw new IllegalArgumentException("Responda todas as questões antes de enviar.");
        }
        int correctCount = (int) attemptAnswers.stream().filter(StudyAttemptAnswerEntity::isCorrect).count();
        attempt.submit(attemptAnswers, correctCount);
        return toAttemptResponse(attempt);
    }

    @Override
    @Transactional
    public void projectGeneratedProblems(StudyProblemsGeneratedEvent event) {
        if (problemSetRepository.existsByFileUuid(event.fileUuid())) {
            return;
        }
        StudyProblemSet problemSet = downloadProblemSet(event);
        StudyWorkspaceEntity workspace = event.workspaceId() == null
                ? null
                : workspaceRepository.findByIdAndUserId(event.workspaceId(), event.uuidUser()).orElse(null);
        StudyProblemSetEntity entity = new StudyProblemSetEntity(
                event.uuidUser(),
                event.fileUuid(),
                workspace,
                event.originalFileName(),
                event.description(),
                problemSet.documentLanguage(),
                problemSet.studyLanguage() == null ? event.studyLanguage() : problemSet.studyLanguage(),
                problemSet.documentSummary(),
                event.aiProvider(),
                event.aiModel(),
                event.extractedTextS3Path(),
                event.studyProblemsS3Path());
        for (int i = 0; i < problemSet.problems().size(); i++) {
            var problem = problemSet.problems().get(i);
            StudyQuestionEntity question = new StudyQuestionEntity(
                    i + 1,
                    problem.question(),
                    problem.subject(),
                    problem.theme(),
                    problem.difficulty(),
                    problem.generalExplanation());
            for (int j = 0; j < problem.answers().size(); j++) {
                var answer = problem.answers().get(j);
                question.addAnswer(new StudyAnswerEntity(j + 1, answer.answer(), answer.correct(), answer.explanation()));
            }
            entity.addQuestion(question);
        }
        problemSetRepository.save(entity);
    }

    private StudyProblemSet downloadProblemSet(StudyProblemsGeneratedEvent event) {
        try {
            byte[] payload = s3StorageService.downloadFile(event.studyProblemsS3Path()).block();
            if (payload == null) {
                throw new IllegalArgumentException("Arquivo de prova vazio.");
            }
            return objectMapper.readValue(payload, StudyProblemSet.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Não foi possível ler a prova gerada.", exception);
        }
    }

    private WorkspaceResponse toWorkspaceResponse(StudyWorkspaceEntity workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt());
    }

    private ProblemSetSummaryResponse toProblemSetSummary(StudyProblemSetEntity problemSet) {
        return new ProblemSetSummaryResponse(
                problemSet.getId(),
                problemSet.getFileUuid(),
                problemSet.getWorkspace() == null ? null : problemSet.getWorkspace().getId(),
                problemSet.getOriginalFileName(),
                problemSet.getDescription(),
                problemSet.getDocumentLanguage(),
                problemSet.getStudyLanguage(),
                problemSet.getQuestions().size(),
                problemSet.getCreatedAt());
    }

    private ProblemSetDetailResponse toProblemSetDetail(StudyProblemSetEntity problemSet, boolean revealCorrect) {
        return new ProblemSetDetailResponse(
                problemSet.getId(),
                problemSet.getFileUuid(),
                problemSet.getWorkspace() == null ? null : problemSet.getWorkspace().getId(),
                problemSet.getOriginalFileName(),
                problemSet.getDescription(),
                problemSet.getDocumentLanguage(),
                problemSet.getStudyLanguage(),
                problemSet.getDocumentSummary(),
                problemSet.getQuestions().stream().map(question -> toQuestionResponse(question, revealCorrect)).toList(),
                problemSet.getCreatedAt());
    }

    private QuestionResponse toQuestionResponse(StudyQuestionEntity question, boolean revealCorrect) {
        return new QuestionResponse(
                question.getId(),
                question.getPosition(),
                question.getQuestion(),
                question.getSubject(),
                question.getTheme(),
                question.getDifficulty(),
                question.getGeneralExplanation(),
                question.getAnswers().stream()
                        .map(answer -> new AnswerResponse(
                                answer.getId(),
                                answer.getPosition(),
                                answer.getAnswer(),
                                answer.getExplanation(),
                                revealCorrect ? answer.isCorrect() : null))
                        .toList());
    }

    private AttemptResponse toAttemptResponse(StudyAttemptEntity attempt) {
        return new AttemptResponse(
                attempt.getId(),
                attempt.getProblemSet().getId(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getCorrectCount(),
                attempt.getTotalQuestions(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getAnswers().stream()
                        .sorted(Comparator.comparing(answer -> answer.getQuestion().getPosition()))
                        .map(answer -> new AttemptAnswerResponse(
                                answer.getQuestion().getId(),
                                answer.getSelectedAnswer().getId(),
                                answer.isCorrect()))
                        .toList());
    }

    private <T> T require(Map<UUID, T> values, UUID id, String message) {
        T value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
