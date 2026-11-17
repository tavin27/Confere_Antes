CREATE TABLE call_recordings (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    audio_url TEXT NOT NULL,
    phone_number VARCHAR(30),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_call_recordings_alert
        FOREIGN KEY (alert_id)
        REFERENCES alerts(id)
        ON DELETE CASCADE
);

CREATE TABLE call_transcriptions (
    id BIGSERIAL PRIMARY KEY,
    call_recording_id BIGINT NOT NULL UNIQUE,
    transcription_text TEXT NOT NULL,
    language VARCHAR(20),
    provider VARCHAR(80),
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_call_transcriptions_recording
        FOREIGN KEY (call_recording_id)
        REFERENCES call_recordings(id)
        ON DELETE CASCADE
);

CREATE TABLE risk_analyses (
    id BIGSERIAL PRIMARY KEY,
    call_recording_id BIGINT NOT NULL UNIQUE,
    risk_score INTEGER,
    risk_level VARCHAR(20),
    is_scam BOOLEAN NOT NULL,
    confidence NUMERIC(5, 4),
    recommendation VARCHAR(30),
    provider VARCHAR(80),
    model_version VARCHAR(80),
    raw_response JSONB,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_risk_analyses_recording
        FOREIGN KEY (call_recording_id)
        REFERENCES call_recordings(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_risk_analyses_score
        CHECK (risk_score IS NULL OR risk_score BETWEEN 0 AND 100),
    CONSTRAINT ck_risk_analyses_confidence
        CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1)
);

CREATE TABLE risk_signals (
    id BIGSERIAL PRIMARY KEY,
    risk_analysis_id BIGINT NOT NULL,
    signal_type VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    evidence_text TEXT,

    CONSTRAINT fk_risk_signals_analysis
        FOREIGN KEY (risk_analysis_id)
        REFERENCES risk_analyses(id)
        ON DELETE CASCADE
);

CREATE TABLE user_decisions (
    id BIGSERIAL PRIMARY KEY,
    call_recording_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    decision VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_user_decisions_recording
        FOREIGN KEY (call_recording_id)
        REFERENCES call_recordings(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_decisions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_call_recordings_alert_id ON call_recordings(alert_id);
CREATE INDEX idx_risk_signals_analysis_id ON risk_signals(risk_analysis_id);
CREATE INDEX idx_user_decisions_user_id ON user_decisions(user_id);
