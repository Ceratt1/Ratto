export interface Workspace {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProblemSetSummary {
  id: string;
  fileUuid: string;
  workspaceId?: string | null;
  originalFileName: string;
  description?: string;
  documentLanguage: string;
  studyLanguage: string;
  questionCount: number;
  createdAt: string;
}

export interface ProblemSetDetail extends ProblemSetSummary {
  documentSummary: string;
  questions: StudyQuestion[];
}

export interface WorkspacePerformance {
  totalProblemSets: number;
  totalAttempts: number;
  answeredQuestions: number;
  correctAnswers: number;
  wrongAnswers: number;
  scorePercent: string | number;
  problemSets: ProblemSetPerformanceSummary[];
}

export interface ProblemSetPerformanceSummary {
  problemSetId: string;
  fileName: string;
  attemptCount: number;
  questionCount: number;
  answeredCount: number;
  correctCount: number;
  wrongCount: number;
  scorePercent: string | number;
  attempts: AttemptPerformanceSummary[];
  subjects: string[];
}

export interface AttemptPerformanceSummary {
  attemptId: string;
  startedAt: string;
  submittedAt?: string | null;
  answeredCount: number;
  correctCount: number;
  wrongCount: number;
  scorePercent: string | number;
  status: "IN_PROGRESS" | "SUBMITTED";
}

export interface ProblemSetPerformance {
  problemSetId: string;
  fileName: string;
  description?: string | null;
  documentSummary: string;
  attemptCount: number;
  questionCount: number;
  answeredCount: number;
  correctCount: number;
  wrongCount: number;
  scorePercent: string | number;
  subjects: PerformanceBreakdown[];
  themes: PerformanceBreakdown[];
  questions: QuestionPerformance[];
}

export interface PerformanceBreakdown {
  name: string;
  answeredCount: number;
  correctCount: number;
  wrongCount: number;
  scorePercent: string | number;
}

export interface QuestionPerformance {
  questionId: string;
  question: string;
  subject: string;
  theme: string;
  difficulty: string;
  correctCount: number;
  wrongCount: number;
  lastSelectedAnswerId?: string | null;
  lastSelectedAnswer?: string | null;
  correctAnswerId: string;
  correctAnswer: string;
  explanation: string;
}

export interface StudyQuestion {
  id: string;
  position: number;
  question: string;
  subject: string;
  theme: string;
  difficulty: string;
  generalExplanation: string;
  answers: StudyAnswer[];
}

export interface StudyAnswer {
  id: string;
  position: number;
  answer: string;
  explanation: string;
  correct?: boolean | null;
}

export interface Attempt {
  id: string;
  problemSetId: string;
  status: "IN_PROGRESS" | "SUBMITTED";
  score?: string | number | null;
  correctCount?: number | null;
  totalQuestions: number;
  startedAt: string;
  submittedAt?: string | null;
  answers: AttemptAnswer[];
}

export interface AttemptAnswer {
  questionId: string;
  selectedAnswerId: string;
  correct: boolean;
}

export interface AnswerAttemptQuestionResponse {
  questionId: string;
  selectedAnswerId: string;
  correctAnswerId: string;
  correct: boolean;
  selectedExplanation: string;
  generalExplanation: string;
  answeredCount: number;
  correctCount: number;
  totalQuestions: number;
  score: string | number;
  status: "IN_PROGRESS" | "SUBMITTED";
}

export type ProcessingStatus = "QUEUED" | "READING" | "GENERATING" | "READY" | "FAILED";

export interface ProcessingStatusResponse {
  fileUuid: string;
  status: ProcessingStatus;
  message: string;
  failedReason?: string | null;
}

export interface PendingProblemSet {
  fileUuid: string;
  uuidRequest: string;
  uuidUser: string;
  fileName: string;
  workspaceId?: string | null;
  status: ProcessingStatus;
  message: string;
  failedReason?: string | null;
  createdAt: string;
}
