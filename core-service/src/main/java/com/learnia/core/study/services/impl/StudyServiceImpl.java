package com.learnia.core.study.services.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learnia.core.study.api.dtos.StudyDtos.AnswerResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AnswerAttemptQuestionRequest;
import com.learnia.core.study.api.dtos.StudyDtos.AnswerAttemptQuestionResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AttemptAnswerResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AttemptPerformanceSummaryResponse;
import com.learnia.core.study.api.dtos.StudyDtos.AttemptResponse;
import com.learnia.core.study.api.dtos.StudyDtos.MoveProblemSetRequest;
import com.learnia.core.study.api.dtos.StudyDtos.PerformanceAnalysisReferenceResponse;
import com.learnia.core.study.api.dtos.StudyDtos.PerformanceAnalysisResponse;
import com.learnia.core.study.api.dtos.StudyDtos.PerformanceBreakdownResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetDetailResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetPerformanceResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetPerformanceSummaryResponse;
import com.learnia.core.study.api.dtos.StudyDtos.ProblemSetSummaryResponse;
import com.learnia.core.study.api.dtos.StudyDtos.QuestionPerformanceResponse;
import com.learnia.core.study.api.dtos.StudyDtos.QuestionResponse;
import com.learnia.core.study.api.dtos.StudyDtos.SubmitAttemptRequest;
import com.learnia.core.study.api.dtos.StudyDtos.WorkspacePerformanceResponse;
import com.learnia.core.study.api.dtos.WorkspaceDtos.WorkspaceRequest;
import com.learnia.core.study.api.dtos.WorkspaceDtos.WorkspaceResponse;
import com.learnia.core.study.repositories.JpaStudyAttemptRepository;
import com.learnia.core.study.repositories.JpaStudyPerformanceAnalysisRepository;
import com.learnia.core.study.repositories.JpaStudyProblemSetRepository;
import com.learnia.core.study.repositories.JpaStudyWorkspaceRepository;
import com.learnia.core.study.repositories.entities.StudyAnswerEntity;
import com.learnia.core.study.repositories.entities.StudyAttemptAnswerEntity;
import com.learnia.core.study.repositories.entities.StudyAttemptEntity;
import com.learnia.core.study.repositories.entities.StudyPerformanceAnalysisEntity;
import com.learnia.core.study.repositories.entities.StudyPerformanceAnalysisEntity.JsonFields;
import com.learnia.core.study.repositories.entities.StudyProblemSetEntity;
import com.learnia.core.study.repositories.entities.StudyQuestionEntity;
import com.learnia.core.study.repositories.entities.StudyWorkspaceEntity;
import com.learnia.events.EventIdFactory;
import com.learnia.events.EventMetadata;
import com.learnia.events.EventTopics;
import com.learnia.events.EventTypes;
import com.learnia.events.StudyProblemsGeneratedEvent;
import com.learnia.events.StudyPerformanceAnalysisFailedEvent;
import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent;
import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.AnswerSnapshot;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.AttemptAnswerSnapshot;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.AttemptSnapshot;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.PerformanceAnalysisSnapshot;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.ProblemSetSnapshot;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.QuestionSnapshot;
import com.learnia.models.study.StudyAnswer;
import com.learnia.models.study.StudyProblemSet;
import com.learnia.tools.aws.service.S3StorageService;

import tools.jackson.databind.ObjectMapper;

@Service
public class StudyServiceImpl implements com.learnia.core.study.services.StudyService {

    private static final Logger logger = LoggerFactory.getLogger(StudyServiceImpl.class);

    private final JpaStudyWorkspaceRepository workspaceRepository;
    private final JpaStudyProblemSetRepository problemSetRepository;
    private final JpaStudyAttemptRepository attemptRepository;
    private final JpaStudyPerformanceAnalysisRepository analysisRepository;
    private final KafkaTemplate<String, StudyPerformanceAnalysisRequestedEvent> performanceAnalysisKafkaTemplate;
    private final S3StorageService s3StorageService;
    private final ObjectMapper objectMapper;

    public StudyServiceImpl(
            JpaStudyWorkspaceRepository workspaceRepository,
            JpaStudyProblemSetRepository problemSetRepository,
            JpaStudyAttemptRepository attemptRepository,
            JpaStudyPerformanceAnalysisRepository analysisRepository,
            KafkaTemplate<String, StudyPerformanceAnalysisRequestedEvent> performanceAnalysisKafkaTemplate,
            S3StorageService s3StorageService,
            ObjectMapper objectMapper) {
        this.workspaceRepository = workspaceRepository;
        this.problemSetRepository = problemSetRepository;
        this.attemptRepository = attemptRepository;
        this.analysisRepository = analysisRepository;
        this.performanceAnalysisKafkaTemplate = performanceAnalysisKafkaTemplate;
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
    @Transactional(readOnly = true)
    public WorkspacePerformanceResponse getWorkspacePerformance(UUID userId, UUID workspaceId) {
        List<StudyProblemSetEntity> problemSets = workspaceId == null
                ? problemSetRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : problemSetRepository.findByUserIdAndWorkspace_IdOrderByCreatedAtDesc(userId, workspaceId);
        Map<UUID, List<StudyAttemptEntity>> attemptsByProblemSet = attemptsByProblemSet(userId, problemSets);
        List<ProblemSetPerformanceSummaryResponse> summaries = problemSets.stream()
                .map(problemSet -> toProblemSetPerformanceSummary(
                        problemSet,
                        attemptsByProblemSet.getOrDefault(problemSet.getId(), List.of())))
                .toList();
        int totalAttempts = summaries.stream().mapToInt(ProblemSetPerformanceSummaryResponse::attemptCount).sum();
        int answeredQuestions = summaries.stream().mapToInt(ProblemSetPerformanceSummaryResponse::answeredCount).sum();
        int correctAnswers = summaries.stream().mapToInt(ProblemSetPerformanceSummaryResponse::correctCount).sum();
        int wrongAnswers = summaries.stream().mapToInt(ProblemSetPerformanceSummaryResponse::wrongCount).sum();
        return new WorkspacePerformanceResponse(
                problemSets.size(),
                totalAttempts,
                answeredQuestions,
                correctAnswers,
                wrongAnswers,
                scorePercent(correctAnswers, answeredQuestions),
                summaries);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemSetPerformanceResponse getProblemSetPerformance(UUID userId, UUID problemSetId) {
        StudyProblemSetEntity problemSet = problemSetRepository.findWithQuestionsByIdAndUserId(problemSetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada."));
        List<StudyAttemptEntity> attempts = attemptRepository.findWithAnswersByUserIdAndProblemSetIds(
                userId,
                List.of(problemSetId));
        return toProblemSetPerformance(problemSet, attempts);
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
        requestPerformanceAnalysisIfSubmitted(attempt);
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
        requestPerformanceAnalysisIfSubmitted(attempt);
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
            List<StudyAnswer> orderedAnswers = orderedAnswersForQuestion(problem.answers(), event.fileUuid(), i);
            for (int j = 0; j < orderedAnswers.size(); j++) {
                var answer = orderedAnswers.get(j);
                question.addAnswer(new StudyAnswerEntity(j + 1, answer.answer(), answer.correct(), answer.explanation()));
            }
            entity.addQuestion(question);
        }
        problemSetRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PerformanceAnalysisResponse getLatestPerformanceAnalysis(UUID userId, UUID problemSetId) {
        problemSetRepository.findByIdAndUserId(problemSetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada."));
        return analysisRepository.findFirstByUserIdAndProblemSet_IdOrderByRequestedAtDesc(userId, problemSetId)
                .map(this::toPerformanceAnalysisResponse)
                .orElse(new PerformanceAnalysisResponse(
                        null,
                        "NOT_REQUESTED",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        null,
                        null,
                        0));
    }

    @Override
    @Transactional
    public PerformanceAnalysisResponse retryPerformanceAnalysis(UUID userId, UUID problemSetId) {
        problemSetRepository.findByIdAndUserId(problemSetId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Prova não encontrada."));
        StudyPerformanceAnalysisEntity latestAnalysis = analysisRepository
                .findFirstByUserIdAndProblemSet_IdOrderByRequestedAtDesc(userId, problemSetId)
                .orElse(null);
        if (latestAnalysis != null && "PENDING".equals(latestAnalysis.getStatus())) {
            return toPerformanceAnalysisResponse(latestAnalysis);
        }
        StudyAttemptEntity latestAttempt = attemptRepository
                .findSubmittedByUserIdAndProblemSetIdOrderBySubmittedAtDesc(userId, problemSetId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Finalize uma prática antes de pedir a leitura de desempenho."));
        return toPerformanceAnalysisResponse(createAndRequestPerformanceAnalysis(latestAttempt));
    }

    @Override
    @Transactional
    public void applyGeneratedPerformanceAnalysis(StudyPerformanceAnalysisGeneratedEvent event) {
        StudyPerformanceAnalysisEntity analysis = analysisRepository.findByAnalysisRequestId(event.analysisRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Análise pendente não encontrada."));
        analysis.markReady(
                event.aiProvider(),
                event.aiModel(),
                event.analysis(),
                new JsonFields(
                        toJson(event.analysis().strengths()),
                        toJson(event.analysis().gaps()),
                        toJson(event.analysis().evolution()),
                        toJson(event.analysis().recommendations()),
                        toJson(event.analysis().exercises()),
                        toJson(event.analysis().references())));
    }

    @Override
    @Transactional
    public void applyFailedPerformanceAnalysis(StudyPerformanceAnalysisFailedEvent event) {
        analysisRepository.findByAnalysisRequestId(event.analysisRequestId())
                .ifPresent(analysis -> analysis.markFailed(event.reason()));
    }

    private Map<UUID, List<StudyAttemptEntity>> attemptsByProblemSet(
            UUID userId,
            List<StudyProblemSetEntity> problemSets) {
        if (problemSets.isEmpty()) {
            return Map.of();
        }
        List<UUID> problemSetIds = problemSets.stream().map(StudyProblemSetEntity::getId).toList();
        return attemptRepository.findWithAnswersByUserIdAndProblemSetIds(userId, problemSetIds).stream()
                .collect(Collectors.groupingBy(attempt -> attempt.getProblemSet().getId()));
    }

    private void requestPerformanceAnalysisIfSubmitted(StudyAttemptEntity attempt) {
        if (!"SUBMITTED".equals(attempt.getStatus()) || analysisRepository.existsByAttempt_Id(attempt.getId())) {
            return;
        }
        createAndRequestPerformanceAnalysis(attempt);
    }

    private StudyPerformanceAnalysisEntity createAndRequestPerformanceAnalysis(StudyAttemptEntity attempt) {
        UUID analysisRequestId = UUID.randomUUID();
        int version = analysisRepository.latestVersion(attempt.getProblemSet().getId()) + 1;
        StudyPerformanceAnalysisEntity analysis = analysisRepository.save(new StudyPerformanceAnalysisEntity(
                analysisRequestId,
                attempt.getUserId(),
                attempt.getProblemSet(),
                attempt,
                version));
        StudyPerformanceAnalysisRequestedEvent event = toPerformanceAnalysisRequestedEvent(attempt, analysis);
        try {
            performanceAnalysisKafkaTemplate.send(
                    EventTopics.STUDY_PERFORMANCE_ANALYSIS_REQUESTED,
                    attempt.getProblemSet().getId().toString(),
                    event);
        } catch (RuntimeException exception) {
            analysis.markFailed("Não foi possível preparar a análise desta prática.");
            logger.warn(
                    "Could not publish performance analysis request {} for attempt {}.",
                    analysisRequestId,
                    attempt.getId(),
                    exception);
        }
        return analysis;
    }

    private StudyPerformanceAnalysisRequestedEvent toPerformanceAnalysisRequestedEvent(
            StudyAttemptEntity submittedAttempt,
            StudyPerformanceAnalysisEntity analysis) {
        StudyProblemSetEntity problemSet = submittedAttempt.getProblemSet();
        List<StudyAttemptEntity> attempts = attemptRepository.findWithAnswersByUserIdAndProblemSetIds(
                submittedAttempt.getUserId(),
                List.of(problemSet.getId()));
        PerformanceAnalysisSnapshot previousAnalysis = analysisRepository
                .findFirstByUserIdAndProblemSet_IdAndAnalysisRequestIdNotOrderByRequestedAtDesc(
                        submittedAttempt.getUserId(),
                        problemSet.getId(),
                        analysis.getAnalysisRequestId())
                .map(previous -> new PerformanceAnalysisSnapshot(
                        previous.getAnalysisRequestId(),
                        previous.getStatus(),
                        previous.getSummary(),
                        previous.getMarkdown(),
                        previous.getGeneratedAt()))
                .orElse(null);
        return new StudyPerformanceAnalysisRequestedEvent(
                new EventMetadata(
                        EventIdFactory.forFile(EventTypes.STUDY_PERFORMANCE_ANALYSIS_REQUESTED, analysis.getAnalysisRequestId()),
                        analysis.getAnalysisRequestId(),
                        submittedAttempt.getId(),
                        EventTypes.STUDY_PERFORMANCE_ANALYSIS_REQUESTED,
                        "core-service",
                        "1",
                        OffsetDateTime.now(ZoneOffset.UTC).toString()),
                submittedAttempt.getUserId(),
                problemSet.getId(),
                submittedAttempt.getId(),
                analysis.getAnalysisRequestId(),
                toProblemSetSnapshot(problemSet),
                attempts.stream()
                        .sorted(Comparator.comparing(this::attemptTime))
                        .map(this::toAttemptSnapshot)
                        .toList(),
                previousAnalysis);
    }

    private ProblemSetSnapshot toProblemSetSnapshot(StudyProblemSetEntity problemSet) {
        return new ProblemSetSnapshot(
                problemSet.getId(),
                problemSet.getFileUuid(),
                problemSet.getOriginalFileName(),
                problemSet.getDescription(),
                problemSet.getDocumentLanguage(),
                problemSet.getStudyLanguage(),
                problemSet.getDocumentSummary(),
                problemSet.getQuestions().stream().map(this::toQuestionSnapshot).toList());
    }

    private QuestionSnapshot toQuestionSnapshot(StudyQuestionEntity question) {
        return new QuestionSnapshot(
                question.getId(),
                question.getPosition(),
                question.getQuestion(),
                question.getSubject(),
                question.getTheme(),
                question.getDifficulty(),
                question.getGeneralExplanation(),
                question.getAnswers().stream()
                        .map(answer -> new AnswerSnapshot(
                                answer.getId(),
                                answer.getPosition(),
                                answer.getAnswer(),
                                answer.getExplanation(),
                                answer.isCorrect()))
                        .toList());
    }

    private AttemptSnapshot toAttemptSnapshot(StudyAttemptEntity attempt) {
        return new AttemptSnapshot(
                attempt.getId(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getCorrectCount(),
                attempt.getTotalQuestions(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getAnswers().stream()
                        .sorted(Comparator.comparing(answer -> answer.getQuestion().getPosition()))
                        .map(answer -> new AttemptAnswerSnapshot(
                                answer.getQuestion().getId(),
                                answer.getSelectedAnswer().getId(),
                                answer.isCorrect(),
                                answer.getQuestion().getSubject(),
                                answer.getQuestion().getTheme(),
                                answer.getQuestion().getDifficulty()))
                        .toList());
    }

    private ProblemSetPerformanceSummaryResponse toProblemSetPerformanceSummary(
            StudyProblemSetEntity problemSet,
            List<StudyAttemptEntity> attempts) {
        PerformanceTotals totals = totals(attempts);
        return new ProblemSetPerformanceSummaryResponse(
                problemSet.getId(),
                problemSet.getOriginalFileName(),
                attempts.size(),
                problemSet.getQuestions().size(),
                totals.answeredCount(),
                totals.correctCount(),
                totals.wrongCount(),
                scorePercent(totals.correctCount(), totals.answeredCount()),
                attemptSummaries(attempts),
                topSubjects(problemSet));
    }

    private ProblemSetPerformanceResponse toProblemSetPerformance(
            StudyProblemSetEntity problemSet,
            List<StudyAttemptEntity> attempts) {
        PerformanceTotals totals = totals(attempts);
        List<StudyAttemptAnswerEntity> attemptAnswers = attempts.stream()
                .flatMap(attempt -> attempt.getAnswers().stream())
                .toList();
        return new ProblemSetPerformanceResponse(
                problemSet.getId(),
                problemSet.getOriginalFileName(),
                problemSet.getDescription(),
                problemSet.getDocumentSummary(),
                attempts.size(),
                problemSet.getQuestions().size(),
                totals.answeredCount(),
                totals.correctCount(),
                totals.wrongCount(),
                scorePercent(totals.correctCount(), totals.answeredCount()),
                breakdown(attemptAnswers, answer -> answer.getQuestion().getSubject()),
                breakdown(attemptAnswers, answer -> answer.getQuestion().getTheme()),
                questionPerformance(problemSet, attempts));
    }

    private List<String> topSubjects(StudyProblemSetEntity problemSet) {
        return problemSet.getQuestions().stream()
                .map(StudyQuestionEntity::getSubject)
                .distinct()
                .limit(3)
                .toList();
    }

    private PerformanceTotals totals(List<StudyAttemptEntity> attempts) {
        int answeredCount = attempts.stream().mapToInt(attempt -> attempt.getAnswers().size()).sum();
        int correctCount = (int) attempts.stream()
                .flatMap(attempt -> attempt.getAnswers().stream())
                .filter(StudyAttemptAnswerEntity::isCorrect)
                .count();
        return new PerformanceTotals(answeredCount, correctCount, answeredCount - correctCount);
    }

    private List<AttemptPerformanceSummaryResponse> attemptSummaries(List<StudyAttemptEntity> attempts) {
        return attempts.stream()
                .sorted(Comparator.comparing(this::attemptTime).reversed())
                .map(attempt -> {
                    int answeredCount = attempt.getAnswers().size();
                    int correctCount = (int) attempt.getAnswers().stream()
                            .filter(StudyAttemptAnswerEntity::isCorrect)
                            .count();
                    int wrongCount = answeredCount - correctCount;
                    return new AttemptPerformanceSummaryResponse(
                            attempt.getId(),
                            attempt.getStartedAt(),
                            attempt.getSubmittedAt(),
                            answeredCount,
                            correctCount,
                            wrongCount,
                            scorePercent(correctCount, answeredCount),
                            attempt.getStatus());
                })
                .toList();
    }

    private List<PerformanceBreakdownResponse> breakdown(
            List<StudyAttemptAnswerEntity> attemptAnswers,
            Function<StudyAttemptAnswerEntity, String> classifier) {
        Map<String, List<StudyAttemptAnswerEntity>> grouped = attemptAnswers.stream()
                .collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> {
                    int answeredCount = entry.getValue().size();
                    int correctCount = (int) entry.getValue().stream().filter(StudyAttemptAnswerEntity::isCorrect).count();
                    int wrongCount = answeredCount - correctCount;
                    return new PerformanceBreakdownResponse(
                            entry.getKey(),
                            answeredCount,
                            correctCount,
                            wrongCount,
                            scorePercent(correctCount, answeredCount));
                })
                .sorted(Comparator.comparing(PerformanceBreakdownResponse::answeredCount).reversed())
                .toList();
    }

    private List<QuestionPerformanceResponse> questionPerformance(
            StudyProblemSetEntity problemSet,
            List<StudyAttemptEntity> attempts) {
        Map<UUID, List<AnswerWithAttempt>> answersByQuestion = attempts.stream()
                .flatMap(attempt -> attempt.getAnswers().stream()
                        .map(answer -> new AnswerWithAttempt(answer, attempt)))
                .collect(Collectors.groupingBy(item -> item.answer().getQuestion().getId()));
        return problemSet.getQuestions().stream()
                .map(question -> toQuestionPerformance(question, answersByQuestion.getOrDefault(question.getId(), List.of())))
                .toList();
    }

    private QuestionPerformanceResponse toQuestionPerformance(
            StudyQuestionEntity question,
            List<AnswerWithAttempt> answers) {
        int correctCount = (int) answers.stream().filter(item -> item.answer().isCorrect()).count();
        int wrongCount = answers.size() - correctCount;
        StudyAnswerEntity correctAnswer = question.getAnswers().stream()
                .filter(StudyAnswerEntity::isCorrect)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Questão sem alternativa correta."));
        StudyAttemptAnswerEntity lastAnswer = answers.stream()
                .max(Comparator.comparing(item -> attemptTime(item.attempt())))
                .map(AnswerWithAttempt::answer)
                .orElse(null);
        return new QuestionPerformanceResponse(
                question.getId(),
                question.getQuestion(),
                question.getSubject(),
                question.getTheme(),
                question.getDifficulty(),
                correctCount,
                wrongCount,
                lastAnswer == null ? null : lastAnswer.getSelectedAnswer().getId(),
                lastAnswer == null ? null : lastAnswer.getSelectedAnswer().getAnswer(),
                correctAnswer.getId(),
                correctAnswer.getAnswer(),
                correctAnswer.getExplanation());
    }

    private OffsetDateTime attemptTime(StudyAttemptEntity attempt) {
        return attempt.getSubmittedAt() == null ? attempt.getStartedAt() : attempt.getSubmittedAt();
    }

    private BigDecimal scorePercent(int correctCount, int answeredCount) {
        return answeredCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctCount * 10000L / answeredCount, 2);
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

    private PerformanceAnalysisResponse toPerformanceAnalysisResponse(StudyPerformanceAnalysisEntity analysis) {
        return new PerformanceAnalysisResponse(
                analysis.getAnalysisRequestId(),
                analysis.getStatus(),
                analysis.getSummary(),
                analysis.getMarkdown(),
                readStringList(analysis.getStrengths()),
                readStringList(analysis.getGaps()),
                readStringList(analysis.getEvolution()),
                readStringList(analysis.getRecommendations()),
                readStringList(analysis.getExercises()),
                readReferences(analysis.getReferences()),
                analysis.getFailureReason(),
                analysis.getRequestedAt(),
                analysis.getGeneratedAt(),
                analysis.getVersion());
    }

    private List<String> readStringList(String json) {
        try {
            String[] values = objectMapper.readValue(json == null ? "[]" : json, String[].class);
            return List.of(values);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<PerformanceAnalysisReferenceResponse> readReferences(String json) {
        try {
            Reference[] references = objectMapper.readValue(json == null ? "[]" : json, Reference[].class);
            return java.util.Arrays.stream(references)
                    .map(reference -> new PerformanceAnalysisReferenceResponse(
                            reference.title(),
                            reference.url(),
                            reference.justification()))
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Não foi possível salvar a análise.", exception);
        }
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

    private List<StudyAnswer> orderedAnswersForQuestion(List<StudyAnswer> answers, UUID fileUuid, int questionIndex) {
        List<StudyAnswer> correctAnswers = answers.stream().filter(StudyAnswer::correct).toList();
        if (correctAnswers.size() != 1) {
            throw new IllegalArgumentException("Questão sem alternativa correta única.");
        }
        List<StudyAnswer> incorrectAnswers = new ArrayList<>(answers.stream()
                .filter(answer -> !answer.correct())
                .toList());
        Collections.shuffle(incorrectAnswers, new Random(answerShuffleSeed(fileUuid, questionIndex)));

        int correctPosition = balancedCorrectPosition(fileUuid, questionIndex, answers.size());
        List<StudyAnswer> orderedAnswers = new ArrayList<>(incorrectAnswers);
        orderedAnswers.add(correctPosition, correctAnswers.getFirst());
        return orderedAnswers;
    }

    private int balancedCorrectPosition(UUID fileUuid, int questionIndex, int answerCount) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < answerCount; i++) {
            positions.add(i);
        }
        Collections.shuffle(positions, new Random(answerShuffleSeed(fileUuid, questionIndex / answerCount)));
        return positions.get(questionIndex % answerCount);
    }

    private long answerShuffleSeed(UUID fileUuid, int questionIndex) {
        return (fileUuid.getMostSignificantBits() * 31)
                ^ fileUuid.getLeastSignificantBits()
                ^ questionIndex;
    }

    private record PerformanceTotals(int answeredCount, int correctCount, int wrongCount) {
    }

    private record AnswerWithAttempt(StudyAttemptAnswerEntity answer, StudyAttemptEntity attempt) {
    }
}
