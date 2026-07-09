import type {
  AnswerAttemptQuestionResponse,
  Attempt,
  PerformanceAnalysis,
  ProblemSetPerformance,
  ProblemSetDetail,
  ProblemSetSummary,
  ProcessingStatusResponse,
  WorkspacePerformance,
  Workspace,
} from "@/features/study-app/models/study.models";

export async function listWorkspaces(token: string): Promise<Workspace[]> {
  return studyRequest<Workspace[]>("/api/study/workspaces", { method: "GET" }, token);
}

export async function createWorkspace(token: string, name: string, description?: string): Promise<Workspace> {
  return studyRequest<Workspace>("/api/study/workspaces", {
    method: "POST",
    body: JSON.stringify({ name, description: description || undefined }),
  }, token);
}

export async function updateWorkspace(token: string, id: string, name: string, description?: string): Promise<Workspace> {
  return studyRequest<Workspace>(`/api/study/workspaces/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ name, description: description || undefined }),
  }, token);
}

export async function listProblemSets(token: string, params: { workspaceId?: string; unassigned?: boolean }): Promise<ProblemSetSummary[]> {
  const search = new URLSearchParams();
  if (params.workspaceId) search.set("workspaceId", params.workspaceId);
  if (params.unassigned) search.set("unassigned", "true");
  return studyRequest<ProblemSetSummary[]>(`/api/study/problem-sets?${search.toString()}`, { method: "GET" }, token);
}

export async function moveProblemSet(token: string, id: string, workspaceId: string | null): Promise<ProblemSetSummary> {
  return studyRequest<ProblemSetSummary>(`/api/study/problem-sets/${id}/workspace`, {
    method: "PATCH",
    body: JSON.stringify({ workspaceId }),
  }, token);
}

export async function getProblemSet(token: string, id: string): Promise<ProblemSetDetail> {
  return studyRequest<ProblemSetDetail>(`/api/study/problem-sets/${id}`, { method: "GET" }, token);
}

export async function getWorkspacePerformance(token: string, workspaceId?: string): Promise<WorkspacePerformance> {
  const search = new URLSearchParams();
  if (workspaceId) search.set("workspaceId", workspaceId);
  const query = search.toString();
  return studyRequest<WorkspacePerformance>(`/api/study/performance${query ? `?${query}` : ""}`, { method: "GET" }, token);
}

export async function getProblemSetPerformance(token: string, id: string): Promise<ProblemSetPerformance> {
  return studyRequest<ProblemSetPerformance>(`/api/study/problem-sets/${id}/performance`, { method: "GET" }, token);
}

export async function getLatestPerformanceAnalysis(token: string, id: string): Promise<PerformanceAnalysis> {
  return studyRequest<PerformanceAnalysis>(
    `/api/study/problem-sets/${id}/performance-analysis`,
    { method: "GET" },
    token,
  );
}

export async function retryPerformanceAnalysis(token: string, id: string): Promise<PerformanceAnalysis> {
  return studyRequest<PerformanceAnalysis>(
    `/api/study/problem-sets/${id}/performance-analysis/retry`,
    { method: "POST" },
    token,
  );
}

export async function startAttempt(token: string, problemSetId: string): Promise<Attempt> {
  return studyRequest<Attempt>(`/api/study/problem-sets/${problemSetId}/attempts`, { method: "POST" }, token);
}

export async function submitAttempt(token: string, attemptId: string, answers: Record<string, string>): Promise<Attempt> {
  return studyRequest<Attempt>(`/api/study/attempts/${attemptId}/submit`, {
    method: "POST",
    body: JSON.stringify({
      answers: Object.entries(answers).map(([questionId, answerId]) => ({ questionId, answerId })),
    }),
  }, token);
}

export async function answerAttemptQuestion(
  token: string,
  attemptId: string,
  questionId: string,
  answerId: string,
): Promise<AnswerAttemptQuestionResponse> {
  return studyRequest<AnswerAttemptQuestionResponse>(`/api/study/attempts/${attemptId}/answers`, {
    method: "POST",
    body: JSON.stringify({ questionId, answerId }),
  }, token);
}

export async function getProcessingStatus(token: string, fileUuid: string): Promise<ProcessingStatusResponse> {
  return studyRequest<ProcessingStatusResponse>(
    `/api/study/processing-status?fileUuid=${encodeURIComponent(fileUuid)}`,
    { method: "GET" },
    token,
  );
}

async function studyRequest<T>(path: string, init: RequestInit, token: string): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
    cache: "no-store",
  });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : undefined;
  if (!response.ok) {
    throw new Error(payload?.message ?? "Não foi possível atualizar seus estudos.");
  }
  return payload as T;
}
