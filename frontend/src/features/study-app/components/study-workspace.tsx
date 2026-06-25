"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import Image from "next/image";
import {
  ArrowLeft,
  BookOpenCheck,
  CheckCircle2,
  FileUp,
  Folder,
  FolderPlus,
  Inbox,
  Languages,
  Pencil,
  Play,
  Save,
} from "lucide-react";

import { UploadForm } from "@/features/pdf-upload/components/upload-form";
import type { UploadResult } from "@/features/pdf-upload/models/upload.models";
import { useAuth } from "@/features/auth/components/auth-provider";
import type {
  AnswerAttemptQuestionResponse,
  Attempt,
  PendingProblemSet,
  ProblemSetDetail,
  ProblemSetSummary,
  Workspace,
} from "@/features/study-app/models/study.models";
import {
  answerAttemptQuestion,
  createWorkspace,
  getProcessingStatus,
  getProblemSet,
  listProblemSets,
  listWorkspaces,
  moveProblemSet,
  startAttempt,
  updateWorkspace,
} from "@/features/study-app/services/study-client";

type WorkspaceView = "overview" | "create" | "practice";
const PENDING_STORAGE_KEY = "ratto:pending-problem-sets";
const RATTO_PROCESSING_MESSAGES = [
  "O Ratto está lendo seu PDF...",
  "O Ratto tá cansado de estudar tanto...",
  "Aff, mais um dia de leitura.",
  "Separando o que vira questão boa.",
  "Quase lá, o Ratto achou umas lacunas.",
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
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [answerFeedback, setAnswerFeedback] = useState<AnswerAttemptQuestionResponse | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [quizFinished, setQuizFinished] = useState(false);
  const [answering, setAnswering] = useState(false);
  const [workspaceFormOpen, setWorkspaceFormOpen] = useState(false);
  const [editingWorkspaceId, setEditingWorkspaceId] = useState<string | null>(null);
  const [workspaceName, setWorkspaceName] = useState("");
  const [workspaceDescription, setWorkspaceDescription] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const { getToken, profile } = useAuth();
  const firstName = profile?.firstName || "estudante";
  const selectedWorkspace = workspaces.find((workspace) => workspace.id === selectedWorkspaceId);
  const visibleProblemSets = selectedWorkspaceId ? problemSets : [...problemSets, ...unassigned];
  const visiblePendingProblemSets = pendingProblemSets.filter((pending) =>
    selectedWorkspaceId ? pending.workspaceId === selectedWorkspaceId : true,
  );

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
    if (pendingProblemSets.length === 0) return;
    const interval = window.setInterval(() => {
      setMessageTick((current) => current + 1);
    }, 4200);
    return () => window.clearInterval(interval);
  }, [pendingProblemSets.length]);

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
        let hasReady = false;
        setPendingProblemSets((current) => current.map((pending) => {
          const nextStatus = statuses.find((status) => status.fileUuid === pending.fileUuid);
          if (!nextStatus) return pending;
          if (nextStatus.status === "READY") hasReady = true;
          return {
            ...pending,
            status: nextStatus.status,
            message: nextStatus.message,
            failedReason: nextStatus.failedReason,
          };
        }));
        if (hasReady) {
          await refresh();
        }
      } catch {
        if (!active) return;
      }
    }

    void poll();
    const interval = window.setInterval(() => void poll(), 2500);
    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [getToken, pendingProblemSets, refresh]);

  function startWorkspaceCreate() {
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
      setCurrentQuestionIndex(0);
      setQuizFinished(false);
      setView("practice");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Falha ao abrir prova.");
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

  async function move(problemSetId: string, workspaceId: string | null) {
    setError("");
    try {
      await moveProblemSet(await getToken(), problemSetId, workspaceId);
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
    const questionProgress = Math.round(((currentQuestionIndex + 1) / activeProblemSet.questions.length) * 100);
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
        <div className="quiz-progress-track" aria-label={`${questionProgress}% da prova percorrida`}>
          <span style={{ width: `${Math.max(answeredProgress, questionProgress)}%` }} />
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
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}
      {loading && <p className="route-state">Carregando seus estudos...</p>}

      {workspaceFormOpen && (
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
            Salvar
          </button>
        </form>
      )}

      <div className="study-layout-flat">
        <aside className="folder-rail" id="areas">
          <button
            className={`folder-item ${!selectedWorkspaceId ? "active" : ""}`}
            onClick={() => setSelectedWorkspaceId(undefined)}
            type="button"
          >
            <Inbox size={18} />
            <span>Todas as provas</span>
            <strong>{problemSets.length + unassigned.length}</strong>
          </button>

          {workspaces.map((workspace) => (
            <div className={`folder-item folder-item-row ${workspace.id === selectedWorkspaceId ? "active" : ""}`} key={workspace.id}>
              <button onClick={() => setSelectedWorkspaceId(workspace.id)} type="button">
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
            <button className="icon-button primary" onClick={() => setView("create")} type="button">
              <FileUp size={18} />
              Gerar prova
            </button>
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
            {visibleProblemSets.map((problemSet) => (
              <article className="problem-row" key={problemSet.id}>
                <div className="problem-row-icon">
                  <BookOpenCheck size={20} />
                </div>
                <div className="problem-row-copy">
                  <strong>{problemSet.originalFileName}</strong>
                  <span>{problemSet.description || "Prática criada a partir do seu material."}</span>
                  <small>
                    <Languages size={14} />
                    {problemSet.questionCount} questões · {languageLabel(problemSet.studyLanguage)}
                  </small>
                </div>
                <select
                  aria-label="Mover prova"
                  onChange={(event) => onMoveSelect(problemSet.id, event.target.value)}
                  value={problemSet.workspaceId ?? ""}
                >
                  <option value="">Sem pasta</option>
                  {workspaces.map((workspace) => (
                    <option key={workspace.id} value={workspace.id}>{workspace.name}</option>
                  ))}
                </select>
                <button className="icon-button secondary" onClick={() => openProblemSet(problemSet.id)} type="button">
                  <Play size={17} />
                  Praticar
                </button>
              </article>
            ))}
          </div>
        </section>
      </div>
    </div>
  );

  function onMoveSelect(problemSetId: string, value: string) {
    void move(problemSetId, value || null);
  }
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
