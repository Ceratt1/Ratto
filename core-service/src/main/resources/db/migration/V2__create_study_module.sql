CREATE TABLE study_workspace (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_study_workspace_user ON study_workspace (user_id, created_at DESC);

CREATE TABLE study_problem_set (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    file_uuid UUID NOT NULL UNIQUE,
    workspace_id UUID REFERENCES study_workspace(id) ON DELETE SET NULL,
    original_file_name VARCHAR(255) NOT NULL,
    description VARCHAR(200),
    document_language VARCHAR(32) NOT NULL,
    study_language VARCHAR(16) NOT NULL,
    document_summary TEXT NOT NULL,
    ai_provider VARCHAR(80) NOT NULL,
    ai_model VARCHAR(120) NOT NULL,
    extracted_text_s3_path VARCHAR(1024) NOT NULL,
    study_problems_s3_path VARCHAR(1024) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_study_problem_set_user_workspace ON study_problem_set (user_id, workspace_id, created_at DESC);

CREATE TABLE study_question (
    id UUID PRIMARY KEY,
    problem_set_id UUID NOT NULL REFERENCES study_problem_set(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    question TEXT NOT NULL,
    subject VARCHAR(160) NOT NULL,
    theme VARCHAR(160) NOT NULL,
    difficulty VARCHAR(80) NOT NULL,
    general_explanation TEXT NOT NULL
);

CREATE UNIQUE INDEX ux_study_question_position ON study_question (problem_set_id, position);

CREATE TABLE study_answer (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES study_question(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    answer TEXT NOT NULL,
    correct BOOLEAN NOT NULL,
    explanation TEXT NOT NULL
);

CREATE UNIQUE INDEX ux_study_answer_position ON study_answer (question_id, position);

CREATE TABLE study_attempt (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    problem_set_id UUID NOT NULL REFERENCES study_problem_set(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    score NUMERIC(5,2),
    correct_count INTEGER,
    total_questions INTEGER NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ
);

CREATE INDEX ix_study_attempt_user ON study_attempt (user_id, started_at DESC);

CREATE TABLE study_attempt_answer (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL REFERENCES study_attempt(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES study_question(id) ON DELETE CASCADE,
    selected_answer_id UUID NOT NULL REFERENCES study_answer(id) ON DELETE CASCADE,
    correct BOOLEAN NOT NULL
);

CREATE UNIQUE INDEX ux_study_attempt_answer_question ON study_attempt_answer (attempt_id, question_id);
