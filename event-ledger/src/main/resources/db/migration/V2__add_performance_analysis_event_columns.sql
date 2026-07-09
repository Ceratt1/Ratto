ALTER TABLE event_ledger
    ADD COLUMN problem_set_id UUID GENERATED ALWAYS AS (
        NULLIF(payload ->> 'problemSetId', '')::UUID
    ) STORED,
    ADD COLUMN attempt_id UUID GENERATED ALWAYS AS (
        NULLIF(payload ->> 'attemptId', '')::UUID
    ) STORED,
    ADD COLUMN analysis_request_id UUID GENERATED ALWAYS AS (
        NULLIF(payload ->> 'analysisRequestId', '')::UUID
    ) STORED;

CREATE INDEX idx_event_ledger_problem_set_id ON event_ledger (problem_set_id);
CREATE INDEX idx_event_ledger_attempt_id ON event_ledger (attempt_id);
CREATE INDEX idx_event_ledger_analysis_request_id ON event_ledger (analysis_request_id);
