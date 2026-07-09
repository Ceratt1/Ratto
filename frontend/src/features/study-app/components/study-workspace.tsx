"use client";

import { CSSProperties, FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from "react";
import Image from "next/image";
import {
  ArrowLeft,
  Award,
  BarChart3,
  BookOpenCheck,
  CheckCircle2,
  ChevronRight,
  Clock3,
  FileUp,
  Folder,
  FolderPlus,
  Inbox,
  Languages,
  ListChecks,
  MoreHorizontal,
  Pencil,
  Play,
  Save,
  Star,
  Target,
} from "lucide-react";

import { UploadForm } from "@/features/pdf-upload/components/upload-form";
import type { UploadResult } from "@/features/pdf-upload/models/upload.models";
import { useAuth } from "@/features/auth/components/auth-provider";
import type {
  AnswerAttemptQuestionResponse,
  AttemptPerformanceSummary,
  Attempt,
  PendingProblemSet,
  PerformanceAnalysis,
  PerformanceBreakdown,
  ProblemSetPerformance,
  ProblemSetDetail,
  ProblemSetPerformanceSummary,
  ProblemSetSummary,
  WorkspacePerformance,
  Workspace,
} from "@/features/study-app/models/study.models";
import {
  answerAttemptQuestion,
  createWorkspace,
  getProblemSetPerformance,
  getLatestPerformanceAnalysis,
  getProcessingStatus,
  getProblemSet,
  getWorkspacePerformance,
  listProblemSets,
  listWorkspaces,
  moveProblemSet,
  retryPerformanceAnalysis,
  startAttempt,
  updateWorkspace,
} from "@/features/study-app/services/study-client";

type WorkspaceView = "overview" | "create" | "practice" | "performance";
const PENDING_STORAGE_KEY = "ratto:pending-problem-sets";
const RATTO_PROCESSING_MESSAGES = [
  "O Ratto está lendo seu PDF...",
  "O Ratto tá cansado de estudar tanto...",
  "Aff, mais um dia de leitura.",
  "Separando o que vira questão boa.",
  "Quase lá, o Ratto achou umas lacunas.",
];
const RATTO_ANALYSIS_MESSAGES = [
  "O Ratto está comparando seus acertos e tropeços.",
  "Ele está procurando os temas que mais pedem revisão.",
  "Quase pronto: separando um plano de treino para você.",
  "O Ratto está pensando em como melhorar sua próxima tentativa.",
];

export function StudyWorkspace() {
  const [view, setView] = useState<WorkspaceView>("overview");
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<string | undefined>();
  const [problemSets, setProblemSets] = useState<ProblemSetSummary[]>([]);
  const [unassigned, setUnassigned] = useState<ProblemSetSummary[]>([]);
  const [pendingProblemSets, setPendingProblemSets] = useState<PendingProblemSet[]>([]);
  const [messageTick, setMessageTick] = useState(0);
  const [activeProblemSet, setActiveProblemSet] = useState<ProblemSetDetail | null>(null);
  const [workspacePerformance, setWorkspacePerformance] = useState<WorkspacePerformance | null>(null);
  const [activeProblemSetPerformance, setActiveProblemSetPerformance] = useState<ProblemSetPerformance | null>(null);
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [answerFeedback, setAnswerFeedback] = useState<AnswerAttemptQuestionResponse | null>(null);
  const [performanceAnalysis, setPerformanceAnalysis] = useState<PerformanceAnalysis | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [quizFinished, setQuizFinished] = useState(false);
  const [answering, setAnswering] = useState(false);
  const [performanceLoading, setPerformanceLoading] = useState(false);
  const [analysisRetrying, setAnalysisRetrying] = useState(false);
  const [openProblemSetMenuId, setOpenProblemSetMenuId] = useState<string | null>(null);
  const [workspaceFormOpen, setWorkspaceFormOpen] = useState(false);
  const [editingWorkspaceId, setEditingWorkspaceId] = useState<string | null>(null);
  const [workspaceName, setWorkspaceName] = useState("");
  const [workspaceDescription, setWorkspaceDescription] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const { getToken, profile } = useAuth();
  const firstName = profile?.firstName || "estudante";
  const selectedWorkspace = workspaces.find((workspace) => workspace.id === selectedWorkspaceId);
  const visibleProblemSets = problemSets;
  const visiblePendingProblemSets = pendingProblemSets.filter((pending) =>
    selectedWorkspaceId ? pending.workspaceId === selectedWorkspaceId : true,
  );
  const performanceByProblemSetId = useMemo(() => new Map(
    workspacePerformance?.problemSets.map((problemSet) => [problemSet.problemSetId, problemSet]) ?? [],
  ), [workspacePerformance]);

  const refresh = useCallback(async () => {
    setError("");
    const token = await getToken();
    const [workspaceList, unassignedList, selectedList] = await Promise.all([
      listWorkspaces(token),
      listProblemSets(token, { unassigned: true }),
      listProblemSets(token, selectedWorkspaceId ? { workspaceId: selectedWorkspaceId } : {}),
    ]);
    setWorkspaces(workspaceList);
    setUnassigned(unassignedList);
    setProblemSets(selectedList);
    return [...selectedList, ...unassignedList];
  }, [getToken, selectedWorkspaceId]);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const token = await getToken();
        const [workspaceList, unassignedList, selectedList] = await Promise.all([
          listWorkspaces(token),
          listProblemSets(token, { unassigned: true }),
          listProblemSets(token, selectedWorkspaceId ? { workspaceId: selectedWorkspaceId } : {}),
        ]);
        if (!active) return;
        setWorkspaces(workspaceList);
        setUnassigned(unassignedList);
        setProblemSets(selectedList);
      } catch (requestError) {
        if (active) {
          setError(requestError instanceof Error ? requestError.message : "Falha ao carregar estudos.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, [getToken, selectedWorkspaceId]);

  useEffect(() => {
    if (!profile?.id) return;
    let active = true;
    async function loadStoredPending() {
      await Promise.resolve();
      if (!active || !profile?.id) return;
      setPendingProblemSets(readStoredPendingProblemSets().filter((pending) => pending.uuidUser === profile.id));
    }
    void loadStoredPending();
    return () => {
      active = false;
    };
  }, [profile?.id]);

  useEffect(() => {
    if (!profile?.id) return;
    writeStoredPendingProblemSets(profile.id, pendingProblemSets);
  }, [pendingProblemSets, profile?.id]);

  useEffect(() => {
    if (view !== "overview") return;
    let active = true;
    async function loadOverviewPerformance() {
      try {
        const token = await getToken();
        const performance = await getWorkspacePerformance(token, selectedWorkspaceId);
        if (active) {
          setWorkspacePerformance(performance);
        }
      } catch {
        if (active) {
          setWorkspacePerformance(null);
        }
      }
    }
    void loadOverviewPerformance();
    return () => {
      active = false;
    };
  }, [getToken, selectedWorkspaceId, view]);

  useEffect(() => {
    if (pendingProblemSets.length === 0) return;
    const interval = window.setInterval(() => {
      setMessageTick((current) => current + 1);
    }, 4200);
    return () => window.clearInterval(interval);
  }, [pendingProblemSets.length]);

  useEffect(() => {
    const showingAnalysisPanel = quizFinished || (view === "performance" && Boolean(activeProblemSetPerformance));
    if (!showingAnalysisPanel || ["READY", "FAILED"].includes(performanceAnalysis?.status ?? "")) return;
    const interval = window.setInterval(() => {
      setMessageTick((current) => current + 1);
    }, 4200);
    return () => window.clearInterval(interval);
  }, [activeProblemSetPerformance, performanceAnalysis?.status, quizFinished, view]);

  useEffect(() => {
    const readyFileUuids = new Set([...problemSets, ...unassigned].map((problemSet) => problemSet.fileUuid));
    if (readyFileUuids.size === 0) return;
    let active = true;
    async function pruneReadyPending() {
      await Promise.resolve();
      if (!active) return;
      setPendingProblemSets((current) => current.filter((pending) => !readyFileUuids.has(pending.fileUuid)));
    }
    void pruneReadyPending();
    return () => {
      active = false;
    };
  }, [problemSets, unassigned]);

  useEffect(() => {
    const activePending = pendingProblemSets.filter((pending) => pending.status !== "FAILED");
    if (activePending.length === 0) return;

    let active = true;
    async function poll() {
      try {
        const token = await getToken();
        const statuses = await Promise.all(
          activePending.map((pending) => getProcessingStatus(token, pending.fileUuid)),
        );
        if (!active) return;
        const readyFileUuids = new Set(
          statuses.filter((status) => status.status === "READY").map((status) => status.fileUuid),
        );
        setPendingProblemSets((current) => current.map((pending) => {
          const nextStatus = statuses.find((status) => status.fileUuid === pending.fileUuid);
          if (!nextStatus) return pending;
          return {
            ...pending,
            status: nextStatus.status,
            message: nextStatus.message,
            failedReason: nextStatus.failedReason,
          };
        }));
        if (readyFileUuids.size > 0) {
          const refreshedProblemSets = await refresh();
          const listedFileUuids = new Set(refreshedProblemSets.map((problemSet) => problemSet.fileUuid));
          setPendingProblemSets((current) => current.filter((pending) =>
            !readyFileUuids.has(pending.fileUuid) || !listedFileUuids.has(pending.fileUuid),
          ));
        }
      } catch {
        if (!active) return;
      }
    }

    void poll();
    const interval = window.setInterval(() => void poll(), 3000);
    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [getToken, pendingProblemSets, refresh]);

  function startWorkspaceCreate() {
    if (workspaceFormOpen && editingWorkspaceId === null) {
      setWorkspaceFormOpen(false);
      setWorkspaceName("");
      setWorkspaceDescription("");
      return;
    }
    setEditingWorkspaceId(null);
    setWorkspaceName("");
    setWorkspaceDescription("");
    setWorkspaceFormOpen(true);
  }

  function startWorkspaceEdit(workspace: Workspace) {
    setEditingWorkspaceId(workspace.id);
    setWorkspaceName(workspace.name);
    setWorkspaceDescription(workspace.description ?? "");
    setWorkspaceFormOpen(true);
  }

  async function saveWorkspace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    try {
      const token = await getToken();
      const workspace = editingWorkspaceId
        ? await updateWorkspace(token, editingWorkspaceId, workspaceName, workspaceDescription)
        : await createWorkspace(token, workspaceName, workspaceDescription);
      setSelectedWorkspaceId(workspace.id);
      setWorkspaceName("");
      setWorkspaceDescription("");
      setEditingWorkspaceId(null);
      setWorkspaceFormOpen(false);
      await refresh();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Falha ao salvar área.");
    }
  }

  async function openProblemSet(problemSetId: string) {
    setError("");
    try {
      const token = await getToken();
      const detail = await getProblemSet(token, problemSetId);
      const nextAttempt = await startAttempt(token, problemSetId);
      setActiveProblemSet(detail);
      setAttempt(nextAttempt);
      setAnswers({});
      setAnswerFeedback(null);
      setPerformanceAnalysis(null);
      setCurrentQuestionIndex(0);
      setQuizFinished(false);
      setView("practice");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Falha ao abrir prova.");
    }
  }

  async function openPerformance() {
    setError("");
    setPerformanceLoading(true);
    setActiveProblemSetPerformance(null);
    setPerformanceAnalysis(null);
    try {
      const token = await getToken();
      setWorkspacePerformance(await getWorkspacePerformance(token, selectedWorkspaceId));
      setView("performance");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Falha ao abrir desempenho.");
    } finally {
      setPerformanceLoading(false);
    }
  }

  async function openProblemSetPerformance(problemSetId: string) {
    setError("");
    setPerformanceLoading(true);
    setPerformanceAnalysis(null);
    try {
      const token = await getToken();
      const [performance, analysis] = await Promise.all([
        getProblemSetPerformance(token, problemSetId),
        getLatestPerformanceAnalysis(token, problemSetId),
      ]);
      setActiveProblemSetPerformance(performance);
      setPerformanceAnalysis(analysis);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Falha ao abrir detalhes da prova.");
    } finally {
      setPerformanceLoading(false);
    }
  }

  async function retryAnalysis(problemSetId: string) {
    setError("");
    setAnalysisRetrying(true);
    try {
      setPerformanceAnalysis(await retryPerformanceAnalysis(await getToken(), problemSetId));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Não foi possível tentar novamente agora.");
    } finally {
      setAnalysisRetrying(false);
    }
  }

  async function confirmCurrentAnswer() {
    if (!attempt || !activeProblemSet) return;
    const question = activeProblemSet.questions[currentQuestionIndex];
    const answerId = answers[question.id];
    if (!answerId || answerFeedback || answering) return;
    setError("");
    setAnswering(true);
    try {
      const feedback = await answerAttemptQuestion(await getToken(), attempt.id, question.id, answerId);
      setAnswerFeedback(feedback);
      setAttempt((current) => current && {
        ...current,
        status: feedback.status,
        score: feedback.score,
        correctCount: feedback.correctCount,
        answers: [
          ...current.answers.filter((answer) => answer.questionId !== feedback.questionId),
          {
            questionId: feedback.questionId,
            selectedAnswerId: feedback.selectedAnswerId,
            correct: feedback.correct,
          },
        ],
      });
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Não foi possível confirmar essa resposta.");
    } finally {
      setAnswering(false);
    }
  }

  function advanceQuestion() {
    if (!activeProblemSet) return;
    if (currentQuestionIndex >= activeProblemSet.questions.length - 1) {
      setQuizFinished(true);
      return;
    }
    setCurrentQuestionIndex((current) => current + 1);
    setAnswerFeedback(null);
  }

  useEffect(() => {
    if (!quizFinished || !activeProblemSet) return;
    let active = true;
    const problemSetId = activeProblemSet.id;
    async function pollAnalysis() {
      try {
        const analysis = await getLatestPerformanceAnalysis(await getToken(), problemSetId);
        if (active) {
          setPerformanceAnalysis(analysis);
        }
      } catch {
        if (active) {
          setPerformanceAnalysis(null);
        }
      }
    }
    void pollAnalysis();
    const interval = window.setInterval(() => void pollAnalysis(), 3000);
    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [activeProblemSet, getToken, quizFinished]);

  useEffect(() => {
    if (view !== "performance" || !activeProblemSetPerformance || activeProblemSetPerformance.answeredCount === 0) return;
    if (["READY", "FAILED"].includes(performanceAnalysis?.status ?? "")) return;
    let active = true;
    const problemSetId = activeProblemSetPerformance.problemSetId;
    async function pollAnalysis() {
      try {
        const analysis = await getLatestPerformanceAnalysis(await getToken(), problemSetId);
        if (active) {
          setPerformanceAnalysis(analysis);
        }
      } catch {
        if (active) {
          setPerformanceAnalysis(null);
        }
      }
    }
    void pollAnalysis();
    const interval = window.setInterval(() => void pollAnalysis(), 3000);
    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [activeProblemSetPerformance, getToken, performanceAnalysis?.status, view]);

  async function move(problemSetId: string, workspaceId: string | null) {
    setError("");
    try {
      await moveProblemSet(await getToken(), problemSetId, workspaceId);
      setOpenProblemSetMenuId(null);
      await refresh();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Falha ao mover prova.");
    }
  }

  function handleUploaded(result: UploadResult) {
    const pending = result.files.map((file) => ({
      fileUuid: file.fileUuid,
      uuidRequest: result.uuidRequest,
      uuidUser: result.uuidUser,
      fileName: file.fileName,
      workspaceId: selectedWorkspaceId ?? null,
      status: "QUEUED" as const,
      message: "O Ratto está pegando seus materiais.",
      createdAt: new Date().toISOString(),
    }));
    const merged = mergePendingProblemSets(pending, pendingProblemSets);
    writeStoredPendingProblemSets(result.uuidUser, merged);
    setPendingProblemSets(merged);
    setView("overview");
    void refresh();
  }

  if (view === "create") {
    return (
      <div className="study-board study-board-upload">
        <div className="study-board-top">
          <button className="icon-button secondary" onClick={() => setView("overview")} type="button">
            <ArrowLeft size={18} />
            Voltar
          </button>
          <div>
            <span className="compact-eyebrow">Nova prova</span>
            <h1>Gerar questões com PDF</h1>
            <p>Defina o objetivo, escolha o idioma e envie um material para criar uma prática.</p>
          </div>
        </div>
        <UploadForm workspaceId={selectedWorkspaceId} onUploaded={handleUploaded} />
      </div>
    );
  }

  if (view === "practice" && activeProblemSet && attempt) {
    const currentQuestion = activeProblemSet.questions[currentQuestionIndex];
    const selectedAnswerId = answers[currentQuestion.id]
      ?? attempt.answers.find((answer) => answer.questionId === currentQuestion.id)?.selectedAnswerId;
    const currentFeedback = answerFeedback?.questionId === currentQuestion.id ? answerFeedback : null;
    const answeredCount = currentFeedback?.answeredCount ?? attempt.answers.length;
    const correctCount = currentFeedback?.correctCount
      ?? attempt.correctCount
      ?? attempt.answers.filter((answer) => answer.correct).length;
    const score = Number(currentFeedback?.score ?? attempt.score ?? 0);
    const answeredProgress = Math.round((answeredCount / activeProblemSet.questions.length) * 100);

    if (quizFinished) {
      return (
        <div className="study-board practice-view quiz-shell">
          <div className="quiz-topbar">
            <button className="quiz-back-button" onClick={() => setView("overview")} type="button">
              <ArrowLeft size={17} />
              Voltar
            </button>
            <div>
              <span className="compact-eyebrow">Resultado</span>
              <h1>{activeProblemSet.originalFileName}</h1>
            </div>
          </div>

          <section className="quiz-summary">
            <CheckCircle2 size={34} />
            <span>{Math.round(score)}%</span>
            <h2>{correctCount} de {activeProblemSet.questions.length} acertos</h2>
            <p>Revise as explicações e refaça a prática quando quiser fechar as lacunas.</p>
            <PerformanceAnalysisPanel
              analysis={performanceAnalysis}
              messageTick={messageTick}
              onRetry={() => retryAnalysis(activeProblemSet.id)}
              retrying={analysisRetrying}
            />
            <button className="icon-button primary" onClick={() => setView("overview")} type="button">
              <BookOpenCheck size={18} />
              Voltar para minhas provas
            </button>
          </section>
        </div>
      );
    }

    return (
      <div className="study-board practice-view quiz-shell">
        <div className="quiz-topbar">
          <button className="quiz-back-button" onClick={() => setView("overview")} type="button">
            <ArrowLeft size={18} />
            Voltar
          </button>
          <div>
            <span className="compact-eyebrow">Prática ativa</span>
            <h1>{activeProblemSet.originalFileName}</h1>
            <p>Pergunta {currentQuestionIndex + 1} de {activeProblemSet.questions.length}</p>
          </div>
        </div>

        <div className="quiz-progress-panel">
          <div>
            <span>Acerto</span>
            <strong>{Math.round(score)}%</strong>
          </div>
          <div>
            <span>Respondidas</span>
            <strong>{answeredCount}/{activeProblemSet.questions.length}</strong>
          </div>
          <div>
            <span>Acertos</span>
            <strong>{correctCount}</strong>
          </div>
        </div>
        <div className="quiz-progress-track" aria-label={`${answeredProgress}% da prova respondida`}>
          <span style={{ width: `${answeredProgress}%` }} />
        </div>

        <article className="quiz-card">
          <div className="quiz-question-meta">
            <strong>{currentQuestion.subject}</strong>
            <span>{currentQuestion.theme}</span>
          </div>
          <h2>{currentQuestion.question}</h2>

          <div className="quiz-option-grid">
            {currentQuestion.answers.map((answer, index) => {
              const isSelected = selectedAnswerId === answer.id;
              const isCorrectAnswer = currentFeedback?.correctAnswerId === answer.id;
              const optionClassName = [
                "quiz-option",
                isSelected ? "selected" : "",
                currentFeedback && isCorrectAnswer ? "correct" : "",
                currentFeedback && isSelected && !currentFeedback.correct ? "incorrect" : "",
              ].filter(Boolean).join(" ");
              return (
                <button
                  className={optionClassName}
                  disabled={Boolean(currentFeedback)}
                  key={answer.id}
                  onClick={() => setAnswers((current) => ({ ...current, [currentQuestion.id]: answer.id }))}
                  type="button"
                >
                  <span>{answerLetter(index)}</span>
                  <strong>{answer.answer}</strong>
                </button>
              );
            })}
          </div>

          {currentFeedback && (
            <div className={`quiz-feedback ${currentFeedback.correct ? "correct" : "incorrect"}`}>
              <strong>{currentFeedback.correct ? "Boa, acertou." : "Quase, essa escapou."}</strong>
              <p>{currentFeedback.selectedExplanation}</p>
              <small>{currentFeedback.generalExplanation}</small>
            </div>
          )}

          <div className="quiz-actions">
            {!currentFeedback ? (
              <button
                className="icon-button primary"
                disabled={!selectedAnswerId || answering}
                onClick={confirmCurrentAnswer}
                type="button"
              >
                <CheckCircle2 size={18} />
                {answering ? "Conferindo..." : "OK"}
              </button>
            ) : (
              <button className="icon-button primary" onClick={advanceQuestion} type="button">
                <Play size={17} />
                {currentQuestionIndex >= activeProblemSet.questions.length - 1 ? "Ver resultado" : "Próxima pergunta"}
              </button>
            )}
          </div>
        </article>
        {error && <p className="error-message">{error}</p>}
      </div>
    );
  }

  if (view === "performance" && workspacePerformance) {
    if (activeProblemSetPerformance) {
      const detailInsights = problemSetPerformanceInsights(activeProblemSetPerformance);
      const detailSnapshot = buildProblemSetPerformanceSnapshot(activeProblemSetPerformance);
      return (
        <div className="study-board performance-view">
          <div className="study-board-top performance-top">
            <button className="icon-button secondary" onClick={() => setActiveProblemSetPerformance(null)} type="button">
              <ArrowLeft size={18} />
              Voltar
            </button>
            <div>
              <span className="compact-eyebrow">Desempenho da prova</span>
              <h1>{activeProblemSetPerformance.fileName}</h1>
              <p>{activeProblemSetPerformance.description || "Veja seus acertos, erros e temas que pedem revisão."}</p>
            </div>
          </div>

          {error && <p className="error-message">{error}</p>}
          {performanceLoading && <p className="route-state">Carregando desempenho...</p>}

          <PerformanceOutcomePanel
            correctCount={activeProblemSetPerformance.correctCount}
            label="Mapa dessa prova"
            subtitle={detailInsights.headline}
            title={`${detailInsights.score}% de aproveitamento`}
            wrongCount={activeProblemSetPerformance.wrongCount}
          />

          <div className="performance-summary-grid">
            <PerformanceStat label="Aproveitamento" value={`${Math.round(toNumber(activeProblemSetPerformance.scorePercent))}%`} />
            <PerformanceStat label="Acertos" tone="correct" value={`${activeProblemSetPerformance.correctCount}`} />
            <PerformanceStat label="Erros" tone="incorrect" value={`${activeProblemSetPerformance.wrongCount}`} />
            <PerformanceStat label="Práticas" value={`${activeProblemSetPerformance.attemptCount}`} />
          </div>

          {activeProblemSetPerformance.answeredCount > 0 && (
            <PerformanceAnalysisPanel
              analysis={performanceAnalysis}
              messageTick={messageTick}
              onRetry={() => retryAnalysis(activeProblemSetPerformance.problemSetId)}
              retrying={analysisRetrying}
            />
          )}

          <PerformanceInsightGrid
            items={[
              {
                icon: <Award size={18} />,
                label: "Melhor assunto",
                title: detailInsights.bestSubject?.name ?? "Ainda sem destaque",
                description: detailInsights.bestSubject
                  ? `${detailInsights.bestSubject.correctCount} acertos em ${detailInsights.bestSubject.answeredCount} respostas.`
                  : "Responda mais questões para aparecer seu ponto forte.",
              },
              {
                icon: <Target size={18} />,
                label: "Revisão primeiro",
                title: detailInsights.weakestSubject?.name ?? "Sem alerta ainda",
                description: detailInsights.weakestSubject
                  ? `${detailInsights.weakestSubject.wrongCount} erros para revisar com calma.`
                  : "Quando houver erros, o Ratto mostra por onde começar.",
                tone: "attention",
              },
              {
                icon: <ListChecks size={18} />,
                label: "Questões respondidas",
                title: `${activeProblemSetPerformance.answeredCount}/${activeProblemSetPerformance.questionCount}`,
                description: `${detailSnapshot.answeredQuestionRate}% da prova já tem histórico de prática.`,
              },
            ]}
          />

          <section className="performance-section">
            <div className="section-title-row">
              <div>
                <span className="compact-eyebrow">Assuntos</span>
                <h2>Leitura por assunto</h2>
              </div>
            </div>
            <BreakdownList items={activeProblemSetPerformance.subjects} empty="Responda questões para ver seu desempenho por assunto." />
          </section>

          <section className="performance-section">
            <div className="section-title-row">
              <div>
                <span className="compact-eyebrow">Temas</span>
                <h2>Temas que merecem atenção</h2>
              </div>
            </div>
            <BreakdownList items={activeProblemSetPerformance.themes} empty="Os temas aparecem depois das primeiras respostas." />
          </section>

          <section className="performance-section">
            <div className="section-title-row">
              <div>
                <span className="compact-eyebrow">Questões</span>
                <h2>O que revisar depois</h2>
              </div>
            </div>
            <div className="question-performance-list">
              {detailInsights.questionReviewOrder.map((question) => (
                <article className="question-performance-card" key={question.questionId}>
                  <div className="quiz-question-meta">
                    <strong>{question.subject}</strong>
                    <span>{question.theme}</span>
                    <span>{question.difficulty}</span>
                  </div>
                  <h3>{question.question}</h3>
                  <div className="question-performance-counts">
                    <span className="correct">{question.correctCount} acertos</span>
                    <span className="incorrect">{question.wrongCount} erros</span>
                  </div>
                  {question.lastSelectedAnswer && (
                    <p><strong>Última resposta:</strong> {question.lastSelectedAnswer}</p>
                  )}
                  <p><strong>Resposta correta:</strong> {question.correctAnswer}</p>
                  <small>{question.explanation}</small>
                </article>
              ))}
            </div>
          </section>
        </div>
      );
    }

    const workspaceInsights = workspacePerformanceInsights(workspacePerformance);
    const workspaceSnapshot = buildWorkspacePerformanceSnapshot(workspacePerformance);
    return (
      <div className="study-board performance-view">
        <div className="study-board-top performance-top">
          <button className="icon-button secondary" onClick={() => setView("overview")} type="button">
            <ArrowLeft size={18} />
            Voltar
          </button>
          <div>
            <span className="compact-eyebrow">Desempenho</span>
            <h1>{selectedWorkspace ? selectedWorkspace.name : "Todas as provas"}</h1>
            <p>Acompanhe acertos, erros e assuntos de cada PDF praticado.</p>
          </div>
        </div>

        {error && <p className="error-message">{error}</p>}
        {performanceLoading && <p className="route-state">Carregando desempenho...</p>}

        <PerformanceOutcomePanel
          correctCount={workspacePerformance.correctAnswers}
          label="Mapa geral"
          subtitle={workspaceInsights.headline}
          title={`${workspaceInsights.score}% de aproveitamento`}
          wrongCount={workspacePerformance.wrongAnswers}
        />

        <div className="performance-summary-grid">
          <PerformanceStat label="Aproveitamento" value={`${Math.round(toNumber(workspacePerformance.scorePercent))}%`} />
          <PerformanceStat label="Acertos" tone="correct" value={`${workspacePerformance.correctAnswers}`} />
          <PerformanceStat label="Erros" tone="incorrect" value={`${workspacePerformance.wrongAnswers}`} />
          <PerformanceStat label="Práticas" value={`${workspacePerformance.totalAttempts}`} />
        </div>

        <PerformanceInsightGrid
          items={[
            {
              icon: <Award size={18} />,
              label: "Melhor material",
              title: workspaceInsights.bestProblemSet?.fileName ?? "Ainda sem campeão",
              description: workspaceInsights.bestProblemSet
                ? `${Math.round(toNumber(workspaceInsights.bestProblemSet.scorePercent))}% de aproveitamento em ${workspaceInsights.bestProblemSet.answeredCount} respostas.`
                : "Pratique um PDF para aparecer seu melhor resultado.",
            },
            {
              icon: <Target size={18} />,
              label: "Revisar primeiro",
              title: workspaceInsights.weakestProblemSet?.fileName ?? "Sem alerta ainda",
              description: workspaceInsights.weakestProblemSet
                ? `${workspaceInsights.weakestProblemSet.wrongCount} erros registrados nesse material.`
                : "Quando houver erros, os materiais mais importantes aparecem aqui.",
              tone: "attention",
            },
            {
              icon: <Clock3 size={18} />,
              label: "Ritmo de prática",
              title: `${workspaceSnapshot.practicedProblemSets}/${workspacePerformance.totalProblemSets} PDFs praticados`,
              description: `${workspaceSnapshot.coveragePercent}% dos materiais dessa área já têm respostas registradas.`,
            },
          ]}
        />

        <section className="performance-section">
          <div className="section-title-row">
            <div>
              <span className="compact-eyebrow">
                <BarChart3 size={14} />
                PDFs
              </span>
              <h2>Materiais para acompanhar</h2>
            </div>
          </div>

          {workspacePerformance.problemSets.length === 0 ? (
            <div className="empty-study-state">
              <BookOpenCheck size={24} />
              <strong>Nenhuma prova pronta nessa área</strong>
              <p>Gere uma prova com PDF para começar a acompanhar sua evolução.</p>
            </div>
          ) : (
            <div className="performance-pdf-list">
              {workspaceInsights.problemSetsByAttention.map((problemSet) => (
                <PerformancePdfRow
                  key={problemSet.problemSetId}
                  onOpen={() => void openProblemSetPerformance(problemSet.problemSetId)}
                  problemSet={problemSet}
                />
              ))}
            </div>
          )}
        </section>
      </div>
    );
  }

  return (
    <div className="study-board student-workspace">
      <div className="study-board-top">
        <div>
          <span className="compact-eyebrow">Área de estudante</span>
          <h1>Bom estudo, {firstName}.</h1>
          <p>Pastas, provas e revisões em uma área limpa para continuar praticando.</p>
        </div>
        <div className="study-board-actions">
          <button className="icon-button secondary" onClick={startWorkspaceCreate} type="button">
            <FolderPlus size={18} />
            Criar pasta
          </button>
          <button className="icon-button primary" onClick={() => setView("create")} type="button">
            <FileUp size={18} />
            Gerar com PDF
          </button>
          {workspaceFormOpen && (
            <div className="folder-menu-popover">
              <form className="inline-folder-form" onSubmit={saveWorkspace}>
                <FolderPlus size={20} />
                <input
                  maxLength={120}
                  onChange={(event) => setWorkspaceName(event.target.value)}
                  placeholder="Nome da pasta"
                  value={workspaceName}
                />
                <input
                  maxLength={500}
                  onChange={(event) => setWorkspaceDescription(event.target.value)}
                  placeholder="Objetivo de estudo"
                  value={workspaceDescription}
                />
                <button className="icon-button primary" disabled={!workspaceName.trim()} type="submit">
                  <Save size={18} />
                  {editingWorkspaceId ? "Salvar edição" : "Salvar"}
                </button>
              </form>
            </div>
          )}
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}
      {loading && <p className="route-state">Carregando seus estudos...</p>}

      <div className="study-layout-flat">
        <aside className="folder-rail" id="areas">
          <button
            className={`folder-item ${!selectedWorkspaceId ? "active" : ""}`}
            onClick={() => {
              setOpenProblemSetMenuId(null);
              setSelectedWorkspaceId(undefined);
            }}
            type="button"
          >
            <Inbox size={18} />
            <span>Todas as provas</span>
            <strong>{problemSets.length}</strong>
          </button>

          {workspaces.map((workspace) => (
            <div className={`folder-item folder-item-row ${workspace.id === selectedWorkspaceId ? "active" : ""}`} key={workspace.id}>
              <button
                onClick={() => {
                  setOpenProblemSetMenuId(null);
                  setSelectedWorkspaceId(workspace.id);
                }}
                type="button"
              >
                <Folder size={18} />
                <span>{workspace.name}</span>
              </button>
              <button aria-label={`Editar ${workspace.name}`} onClick={() => startWorkspaceEdit(workspace)} type="button">
                <Pencil size={16} />
              </button>
            </div>
          ))}
        </aside>

        <section className="study-main-flat" id="atividade">
          <div className="section-title-row">
            <div>
              <span className="compact-eyebrow">
                <BookOpenCheck size={14} />
                {selectedWorkspace ? selectedWorkspace.name : "Todas as provas"}
              </span>
              <h2>Provas prontas para praticar</h2>
            </div>
            <div className="section-actions">
              {selectedWorkspaceId && (
                <button className="icon-button secondary" onClick={() => void openPerformance()} type="button">
                  <BarChart3 size={18} />
                  Desempenho
                </button>
              )}
              <button className="icon-button primary" onClick={() => setView("create")} type="button">
                <FileUp size={18} />
                Gerar prova
              </button>
            </div>
          </div>

          {visibleProblemSets.length === 0 && visiblePendingProblemSets.length === 0 && (
            <div className="empty-study-state">
              <BookOpenCheck size={24} />
              <strong>Nenhuma prova pronta ainda</strong>
              <p>Gere uma prova com PDF para começar uma prática e revisar lacunas.</p>
            </div>
          )}

          <div className="problem-table">
            {visiblePendingProblemSets.map((pending) => (
              <article className="problem-row pending-problem-row" key={pending.fileUuid}>
                <div className="ratto-processing-mark">
                  <Image
                    alt=""
                    height={52}
                    src={`/ratto-writing/ratto-writing-${(messageTick % 3) + 1}.png`}
                    width={52}
                  />
                  <span className={`processing-led ${pending.status === "FAILED" ? "failed" : pending.status === "READY" ? "ready" : "processing"}`} />
                </div>
                <div className="problem-row-copy">
                  <strong>{pending.fileName}</strong>
                  <span>{processingMessage(pending, messageTick)}</span>
                  {pending.failedReason && <small>{pending.failedReason}</small>}
                </div>
                <span className="processing-status-label">{processingLabel(pending.status)}</span>
                <button className="icon-button secondary" disabled type="button">
                  <Play size={17} />
                  Praticar
                </button>
              </article>
            ))}
            {visibleProblemSets.map((problemSet) => {
              const performance = performanceByProblemSetId.get(problemSet.id);
              const mastered = performance ? hasPerfectAttempt(performance) : false;
              return (
                <article className={`problem-row ${mastered ? "mastered" : ""}`} key={problemSet.id}>
                  <div className="problem-row-icon">
                    <BookOpenCheck size={20} />
                    {mastered && <Star className="mastery-star" fill="currentColor" size={16} />}
                  </div>
                  <div className="problem-row-copy">
                    <strong>
                      {problemSet.originalFileName}
                      {mastered && <span className="mastery-label">100%</span>}
                    </strong>
                    <span>{problemSet.description || "Prática criada a partir do seu material."}</span>
                    <small>
                      <Languages size={14} />
                      {problemSet.questionCount} questões · {languageLabel(problemSet.studyLanguage)}
                    </small>
                    <ProblemSetHoverSummary performance={performance} />
                  </div>
                  <ProblemSetActionsMenu
                    currentWorkspaceId={problemSet.workspaceId ?? null}
                    isOpen={openProblemSetMenuId === problemSet.id}
                    onMove={(workspaceId) => void move(problemSet.id, workspaceId)}
                    onToggle={() => setOpenProblemSetMenuId((current) => current === problemSet.id ? null : problemSet.id)}
                    workspaces={workspaces}
                  />
                  <button className="icon-button secondary" onClick={() => openProblemSet(problemSet.id)} type="button">
                    <Play size={17} />
                    Praticar
                  </button>
                </article>
              );
            })}
          </div>
        </section>
      </div>
    </div>
  );
}

function languageLabel(language: string): string {
  if (language === "en") return "Inglês";
  if (language === "es") return "Espanhol";
  return "Português";
}

function answerLetter(index: number): string {
  return String.fromCharCode(65 + index);
}

function mergePendingProblemSets(nextItems: PendingProblemSet[], currentItems: PendingProblemSet[]): PendingProblemSet[] {
  const existing = new Set(currentItems.map((item) => item.fileUuid));
  return [...nextItems.filter((item) => !existing.has(item.fileUuid)), ...currentItems];
}

function readStoredPendingProblemSets(): PendingProblemSet[] {
  const stored = window.localStorage.getItem(PENDING_STORAGE_KEY);
  if (!stored) return [];
  try {
    const parsed = JSON.parse(stored) as PendingProblemSet[];
    return parsed.filter((item) => item.uuidUser && item.fileUuid);
  } catch {
    window.localStorage.removeItem(PENDING_STORAGE_KEY);
    return [];
  }
}

function writeStoredPendingProblemSets(uuidUser: string, pendingProblemSets: PendingProblemSet[]) {
  const existing = readStoredPendingProblemSets();
  const others = existing.filter((pending) => pending.uuidUser !== uuidUser);
  window.localStorage.setItem(PENDING_STORAGE_KEY, JSON.stringify([...others, ...pendingProblemSets]));
}

function processingMessage(pending: PendingProblemSet, tick: number): string {
  if (pending.status === "FAILED") {
    return "Não deu para preparar essa prova.";
  }
  if (pending.status === "READY") {
    return "Prova pronta, organizando na lista.";
  }
  return RATTO_PROCESSING_MESSAGES[tick % RATTO_PROCESSING_MESSAGES.length];
}

function processingLabel(status: PendingProblemSet["status"]): string {
  if (status === "FAILED") return "Falhou";
  if (status === "READY") return "Pronta";
  if (status === "GENERATING") return "Gerando";
  if (status === "READING") return "Lendo";
  return "Na fila";
}

function ProblemSetActionsMenu({
  currentWorkspaceId,
  isOpen,
  onMove,
  onToggle,
  workspaces,
}: Readonly<{
  currentWorkspaceId: string | null;
  isOpen: boolean;
  onMove: (workspaceId: string | null) => void;
  onToggle: () => void;
  workspaces: Workspace[];
}>) {
  return (
    <div className="problem-actions-menu">
      <button
        aria-expanded={isOpen}
        aria-label="Opções da prova"
        className="problem-menu-trigger"
        onClick={onToggle}
        type="button"
      >
        <MoreHorizontal size={20} />
      </button>
      {isOpen && (
        <div className="problem-menu-popover">
          <strong>Mover para</strong>
          <button
            className={currentWorkspaceId === null ? "active" : ""}
            onClick={() => onMove(null)}
            type="button"
          >
            Sem pasta
          </button>
          {workspaces.map((workspace) => (
            <button
              className={currentWorkspaceId === workspace.id ? "active" : ""}
              key={workspace.id}
              onClick={() => onMove(workspace.id)}
              type="button"
            >
              {workspace.name}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function ProblemSetHoverSummary({ performance }: Readonly<{ performance?: ProblemSetPerformanceSummary }>) {
  if (!performance) {
    return (
      <span className="problem-hover-summary">
        <strong>Resumo da prática</strong>
        <small>Carregando desempenho desse material...</small>
      </span>
    );
  }
  const score = Math.round(toNumber(performance.scorePercent));
  const lastAttempt = performance.attempts[0];
  return (
    <span className="problem-hover-summary">
      <strong>Resumo da prática</strong>
      {performance.attemptCount === 0 ? (
        <small>Ainda sem tentativa. Comece a prática para criar seu histórico.</small>
      ) : (
        <>
          <small>{performance.attemptCount} tentativa{performance.attemptCount === 1 ? "" : "s"} registrada{performance.attemptCount === 1 ? "" : "s"}</small>
          <small>{score}% de acerto · {performance.correctCount} acertos · {performance.wrongCount} erros</small>
          {lastAttempt && (
            <small>Última: {formatAttemptDate(lastAttempt.submittedAt ?? lastAttempt.startedAt)} · {Math.round(toNumber(lastAttempt.scorePercent))}%</small>
          )}
        </>
      )}
    </span>
  );
}

function PerformanceAnalysisPanel({ analysis, messageTick, onRetry, retrying }: Readonly<{
  analysis: PerformanceAnalysis | null;
  messageTick: number;
  onRetry?: () => void;
  retrying?: boolean;
}>) {
  if (analysis?.status === "READY" && analysis.markdown) {
    return (
      <section className="performance-analysis-ready" aria-label="Análise da prática">
        <div className="performance-analysis-heading">
          <Star size={18} />
          <h3>Leitura do seu desempenho</h3>
        </div>
        <div className="performance-analysis-markdown">
          {renderMarkdown(analysis.markdown)}
        </div>
        {analysis.references.length > 0 && (
          <div className="performance-analysis-references">
            <h4>Fontes para revisar</h4>
            <ul>
              {analysis.references.map((reference) => (
                <li key={`${reference.title}-${reference.url}`}>
                  <a href={reference.url} rel="noreferrer" target="_blank">
                    {reference.title || reference.url}
                  </a>
                  {reference.justification && <p>{reference.justification}</p>}
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>
    );
  }

  if (analysis?.status === "FAILED") {
    const canRetry = onRetry && !isQuotaLimitError(analysis.failureReason);
    return (
      <section className="performance-analysis-waiting failed" aria-label="Análise indisponível">
        <div>
          <strong>O Ratto não conseguiu fechar essa leitura agora.</strong>
          <p>{friendlyAnalysisError(analysis.failureReason)}</p>
          {canRetry && (
            <button className="icon-button secondary analysis-retry-button" disabled={retrying} onClick={onRetry} type="button">
              <Clock3 size={17} />
              {retrying ? "Tentando..." : "Tentar de novo"}
            </button>
          )}
        </div>
      </section>
    );
  }

  return (
    <section className="performance-analysis-waiting" aria-label="Análise em preparo">
      <div className="analysis-ratto-mark">
        <Image src="/logo-ratto.png" alt="" width={64} height={64} />
      </div>
      <div>
        <strong>Preparando sua leitura de desempenho</strong>
        <p>{RATTO_ANALYSIS_MESSAGES[messageTick % RATTO_ANALYSIS_MESSAGES.length]}</p>
      </div>
    </section>
  );
}

function friendlyAnalysisError(reason?: string | null): string {
  const normalized = reason?.toLowerCase() ?? "";
  if (isQuotaLimitError(reason)) {
    return "Nesta versão MVP, a leitura automática ficou indisponível por limite de uso. Suas estatísticas, acertos, erros e pontos de revisão continuam salvos aqui.";
  }
  if (normalized.includes("503") || normalized.includes("high demand") || normalized.includes("unavailable")) {
    return "A leitura encontrou muita procura no serviço de IA. Suas respostas ficaram salvas e você pode tentar novamente em instantes.";
  }
  if (normalized.includes("429") || normalized.includes("quota")) {
    return "A leitura bateu um limite temporário de uso. Suas respostas ficaram salvas e você pode tentar novamente daqui a pouco.";
  }
  return "Suas respostas ficaram salvas. Você já pode revisar a prova e tentar gerar a leitura novamente.";
}

function isQuotaLimitError(reason?: string | null): boolean {
  const normalized = reason?.toLowerCase() ?? "";
  return normalized.includes("quota")
    || normalized.includes("resource_exhausted")
    || normalized.includes("free_tier_requests")
    || normalized.includes("exceeded your current quota");
}

function renderMarkdown(markdown: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  let listItems: ReactNode[] = [];

  function flushList() {
    if (listItems.length === 0) return;
    nodes.push(<ul key={`list-${nodes.length}`}>{listItems}</ul>);
    listItems = [];
  }

  markdown.split("\n").forEach((line, index) => {
    const trimmed = line.trim();
    const key = `${index}-${trimmed}`;
    if (!trimmed) {
      flushList();
      return;
    }
    if (trimmed.startsWith("### ")) {
      flushList();
      nodes.push(<h3 key={key}>{renderInlineMarkdown(trimmed.slice(4))}</h3>);
      return;
    }
    if (trimmed.startsWith("## ")) {
      flushList();
      nodes.push(<h2 key={key}>{renderInlineMarkdown(trimmed.slice(3))}</h2>);
      return;
    }
    if (trimmed.startsWith("# ")) {
      flushList();
      nodes.push(<h2 key={key}>{renderInlineMarkdown(trimmed.slice(2))}</h2>);
      return;
    }
    if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
      listItems.push(<li key={key}>{renderInlineMarkdown(trimmed.slice(2))}</li>);
      return;
    }
    flushList();
    nodes.push(<p key={key}>{renderInlineMarkdown(trimmed)}</p>);
  });

  flushList();
  return nodes;
}

function renderInlineMarkdown(text: string): ReactNode[] {
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return parts.filter(Boolean).map((part, index) => {
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={`${part}-${index}`}>{part.slice(2, -2)}</strong>;
    }
    return part;
  });
}

function PerformanceOutcomePanel({
  correctCount,
  label,
  subtitle,
  title,
  wrongCount,
}: Readonly<{ correctCount: number; label: string; subtitle?: string; title?: string; wrongCount: number }>) {
  const total = correctCount + wrongCount;
  const percent = percentage(correctCount, total);
  return (
    <section className="performance-outcome-panel">
      <OutcomeDonut correctCount={correctCount} label={label} wrongCount={wrongCount} />
      <div className="performance-outcome-copy">
        <span className="compact-eyebrow">{label}</span>
        <h2>{total === 0 ? "Comece a responder para ver seu mapa" : title ?? `${percent}% de aproveitamento`}</h2>
        <p>
          {total === 0
            ? "Assim que você praticar, seus acertos e erros aparecem aqui para facilitar a revisão."
            : subtitle ?? `${correctCount} acertos e ${wrongCount} erros em ${total} respostas registradas.`}
        </p>
        <div className="performance-legend">
          <span><i className="correct" /> Acertos</span>
          <span><i className="incorrect" /> Erros</span>
        </div>
      </div>
    </section>
  );
}

function PerformanceInsightGrid({
  items,
}: Readonly<{
  items: Array<{
    description: string;
    icon: ReactNode;
    label: string;
    title: string;
    tone?: "attention";
  }>;
}>) {
  return (
    <section className="performance-insight-grid" aria-label="Resumo do desempenho">
      {items.map((item) => (
        <article className={`performance-insight-card ${item.tone ?? ""}`} key={`${item.label}-${item.title}`}>
          <span className="performance-insight-icon">{item.icon}</span>
          <div>
            <span>{item.label}</span>
            <strong>{item.title}</strong>
            <p>{item.description}</p>
          </div>
        </article>
      ))}
    </section>
  );
}

function PerformancePdfRow({
  onOpen,
  problemSet,
}: Readonly<{ onOpen: () => void; problemSet: ProblemSetPerformanceSummary }>) {
  const score = Math.round(toNumber(problemSet.scorePercent));
  const status = performanceStatus(score, problemSet.answeredCount);
  return (
    <article className={`performance-pdf-row ${status.tone}`}>
      <OutcomeDonut
        correctCount={problemSet.correctCount}
        label={problemSet.fileName}
        small
        wrongCount={problemSet.wrongCount}
      />
      <div className="performance-pdf-copy">
        <div className="performance-pdf-heading">
          <div>
            <strong>{problemSet.fileName}</strong>
            <span>{status.label}</span>
          </div>
          <strong>{score}%</strong>
        </div>
        <span>
          {problemSet.attemptCount === 0
            ? "Ainda sem prática registrada."
            : (
              <AttemptHistoryPopover
                attempts={problemSet.attempts}
                label={`${problemSet.attemptCount} prática${problemSet.attemptCount === 1 ? "" : "s"} feita${problemSet.attemptCount === 1 ? "" : "s"}`}
              />
            )}
        </span>
        <div className="performance-mini-grid">
          <span><strong>{problemSet.correctCount}</strong> acertos</span>
          <span><strong>{problemSet.wrongCount}</strong> erros</span>
          <span><strong>{problemSet.answeredCount}</strong> respostas</span>
          <span><strong>{problemSet.questionCount}</strong> questões</span>
        </div>
        <div className="performance-inline-progress" aria-label={`${score}% de aproveitamento em ${problemSet.fileName}`}>
          <span style={{ width: `${score}%` }} />
        </div>
        <small>{problemSet.subjects.length > 0 ? problemSet.subjects.join(" · ") : "Assuntos aparecem após a geração da prova."}</small>
      </div>
      <button
        className="icon-button secondary"
        onClick={onOpen}
        type="button"
      >
        Ver detalhes
        <ChevronRight size={17} />
      </button>
    </article>
  );
}

function OutcomeDonut({
  correctCount,
  label,
  wrongCount,
  small = false,
}: Readonly<{ correctCount: number; label: string; wrongCount: number; small?: boolean }>) {
  const total = correctCount + wrongCount;
  const percent = percentage(correctCount, total);
  const style = { "--correct-percent": `${percent}%` } as CSSProperties;
  return (
    <div
      aria-label={`${label}: ${correctCount} acertos, ${wrongCount} erros, ${percent}% de aproveitamento`}
      className={`performance-donut ${small ? "small" : ""} ${total === 0 ? "empty" : ""}`}
      style={style}
      tabIndex={0}
    >
      <span>{total === 0 ? "0%" : `${percent}%`}</span>
      <div className="performance-donut-tooltip" role="tooltip">
        <strong>{label}</strong>
        <small>{correctCount} acertos</small>
        <small>{wrongCount} erros</small>
        <small>{total} respostas</small>
      </div>
    </div>
  );
}

function PerformanceStat({ label, tone, value }: Readonly<{ label: string; tone?: "correct" | "incorrect"; value: string }>) {
  return (
    <article className={`performance-stat ${tone ?? ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function BreakdownList({ items, empty }: Readonly<{ items: PerformanceBreakdown[]; empty: string }>) {
  if (items.length === 0) {
    return <p className="empty-state">{empty}</p>;
  }
  return (
    <div className="performance-breakdown-list">
      {items.map((item) => (
        <article className="performance-breakdown-row" key={item.name}>
          <OutcomeDonut correctCount={item.correctCount} label={item.name} small wrongCount={item.wrongCount} />
          <div>
            <strong>{item.name}</strong>
            <span>{item.answeredCount} respostas</span>
          </div>
          <div className="performance-score-pill">{Math.round(toNumber(item.scorePercent))}%</div>
          <small>{item.correctCount} acertos · {item.wrongCount} erros</small>
        </article>
      ))}
    </div>
  );
}

function AttemptHistoryPopover({
  attempts,
  label,
}: Readonly<{ attempts: AttemptPerformanceSummary[]; label: string }>) {
  return (
    <span className="attempt-history-trigger" tabIndex={0}>
      {label}
      <span className="attempt-history-popover">
        <strong>Histórico de práticas</strong>
        {attempts.length === 0 ? (
          <small>Nenhuma prática registrada.</small>
        ) : attempts.map((attempt, index) => (
          <span className="attempt-history-item" key={attempt.attemptId}>
            <span>
              <strong>{index + 1}. {formatAttemptDate(attempt.submittedAt ?? attempt.startedAt)}</strong>
              <small>{attempt.status === "SUBMITTED" ? "Finalizada" : "Em andamento"}</small>
            </span>
            <span>
              {attempt.correctCount}/{attempt.answeredCount} acertos
              <small>{attempt.wrongCount} erros · {Math.round(toNumber(attempt.scorePercent))}%</small>
            </span>
          </span>
        ))}
      </span>
    </span>
  );
}

function toNumber(value: string | number | null | undefined): number {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function workspacePerformanceInsights(performance: WorkspacePerformance) {
  const practicedProblemSets = performance.problemSets.filter((problemSet) => problemSet.answeredCount > 0);
  const problemSetsByAttention = [...performance.problemSets].sort((left, right) => {
    if (left.answeredCount === 0 && right.answeredCount > 0) return 1;
    if (right.answeredCount === 0 && left.answeredCount > 0) return -1;
    if (right.wrongCount !== left.wrongCount) return right.wrongCount - left.wrongCount;
    return toNumber(left.scorePercent) - toNumber(right.scorePercent);
  });
  const bestProblemSet = [...practicedProblemSets].sort((left, right) =>
    toNumber(right.scorePercent) - toNumber(left.scorePercent),
  )[0];
  const weakestProblemSet = [...practicedProblemSets].sort((left, right) => {
    if (right.wrongCount !== left.wrongCount) return right.wrongCount - left.wrongCount;
    return toNumber(left.scorePercent) - toNumber(right.scorePercent);
  })[0];
  const score = Math.round(toNumber(performance.scorePercent));
  return {
    bestProblemSet,
    headline: performance.answeredQuestions === 0
      ? "Quando você praticar, esse espaço vira um mapa simples do que revisar."
      : `${performance.correctAnswers} acertos, ${performance.wrongAnswers} erros e ${performance.totalAttempts} práticas registradas.`,
    problemSetsByAttention,
    score,
    weakestProblemSet,
  };
}

function problemSetPerformanceInsights(performance: ProblemSetPerformance) {
  const bestSubject = bestBreakdown(performance.subjects);
  const weakestSubject = weakestBreakdown(performance.subjects);
  const questionReviewOrder = [...performance.questions].sort((left, right) => {
    if (right.wrongCount !== left.wrongCount) return right.wrongCount - left.wrongCount;
    return left.correctCount - right.correctCount;
  });
  const score = Math.round(toNumber(performance.scorePercent));
  return {
    bestSubject,
    headline: performance.answeredCount === 0
      ? "Responda essa prova para enxergar seus pontos fortes e lacunas."
      : `${performance.correctCount} acertos e ${performance.wrongCount} erros em ${performance.answeredCount} respostas dessa prova.`,
    questionReviewOrder,
    score,
    weakestSubject,
  };
}

function bestBreakdown(items: PerformanceBreakdown[]): PerformanceBreakdown | undefined {
  return items
    .filter((item) => item.answeredCount > 0)
    .sort((left, right) => toNumber(right.scorePercent) - toNumber(left.scorePercent))[0];
}

function weakestBreakdown(items: PerformanceBreakdown[]): PerformanceBreakdown | undefined {
  return items
    .filter((item) => item.answeredCount > 0)
    .sort((left, right) => {
      if (right.wrongCount !== left.wrongCount) return right.wrongCount - left.wrongCount;
      return toNumber(left.scorePercent) - toNumber(right.scorePercent);
    })[0];
}

function buildWorkspacePerformanceSnapshot(performance: WorkspacePerformance) {
  const practicedProblemSets = performance.problemSets.filter((problemSet) => problemSet.answeredCount > 0).length;
  return {
    coveragePercent: percentage(practicedProblemSets, performance.totalProblemSets),
    practicedProblemSets,
    totalAttempts: performance.totalAttempts,
    totalProblemSets: performance.totalProblemSets,
  };
}

function buildProblemSetPerformanceSnapshot(performance: ProblemSetPerformance) {
  const answeredQuestions = performance.questions.filter((question) =>
    question.correctCount + question.wrongCount > 0,
  ).length;
  return {
    answeredQuestionRate: percentage(answeredQuestions, performance.questionCount),
    questionsNeedingReview: performance.questions
      .filter((question) => question.wrongCount > 0)
      .map((question) => ({
        questionId: question.questionId,
        subject: question.subject,
        theme: question.theme,
        wrongCount: question.wrongCount,
      })),
  };
}

function performanceStatus(score: number, answeredCount: number): { label: string; tone: "strong" | "attention" | "quiet" } {
  if (answeredCount === 0) return { label: "Aguardando primeira prática", tone: "quiet" };
  if (score >= 80) return { label: "Indo muito bem", tone: "strong" };
  if (score >= 50) return { label: "Em construção", tone: "quiet" };
  return { label: "Pede revisão", tone: "attention" };
}

function hasPerfectAttempt(problemSet: ProblemSetPerformanceSummary): boolean {
  return problemSet.attempts.some((attempt) =>
    attempt.answeredCount === problemSet.questionCount
    && attempt.correctCount === problemSet.questionCount,
  );
}

function percentage(value: number, total: number): number {
  if (total <= 0) return 0;
  return Math.round((value / total) * 100);
}

function formatAttemptDate(value: string): string {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
  }).format(new Date(value));
}
