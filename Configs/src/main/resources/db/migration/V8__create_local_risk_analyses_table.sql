
CREATE TABLE local_risk_analyses (
    id BIGSERIAL PRIMARY KEY,
    call_recording_id BIGINT NOT NULL UNIQUE,
    risk_score INTEGER NOT NULL,
    alert_triggered BOOLEAN NOT NULL,
    detected_rules JSONB NOT NULL DEFAULT '[]'::jsonb,
    reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_local_risk_analyses_recording
        FOREIGN KEY (call_recording_id)
        REFERENCES call_recordings(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_local_risk_analyses_score
        CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_local_risk_analyses_recording_id
    ON local_risk_analyses(call_recording_id);
