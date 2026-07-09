CREATE TABLE study_performance_analysis (
    id UUID PRIMARY KEY,
    analysis_request_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    problem_set_id UUID NOT NULL REFERENCES study_problem_set(id) ON DELETE CASCADE,
    attempt_id UUID NOT NULL REFERENCES study_attempt(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    summary TEXT,
    markdown TEXT,
    strengths JSONB NOT NULL DEFAULT '[]'::jsonb,
    gaps JSONB NOT NULL DEFAULT '[]'::jsonb,
    evolution JSONB NOT NULL DEFAULT '[]'::jsonb,
    recommendations JSONB NOT NULL DEFAULT '[]'::jsonb,
    exercises JSONB NOT NULL DEFAULT '[]'::jsonb,
    analysis_references JSONB NOT NULL DEFAULT '[]'::jsonb,
    failure_reason TEXT,
    ai_provider VARCHAR(80),
    ai_model VARCHAR(120),
    requested_at TIMESTAMPTZ NOT NULL,
    generated_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_study_performance_analysis_version
    ON study_performance_analysis (problem_set_id, version);
CREATE INDEX ix_study_performance_analysis_latest
    ON study_performance_analysis (user_id, problem_set_id, requested_at DESC);
CREATE INDEX ix_study_performance_analysis_status
    ON study_performance_analysis (status);
