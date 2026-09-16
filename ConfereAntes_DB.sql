CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE transactions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type_transaction         VARCHAR(40) NOT NULL,   -- 'cancelamento_boleto' | 'correcao_compra' | 'instalacao_app' | 'outro'
    amount                   NUMERIC(12,2),
    description_transaction  TEXT,                    -- descrição gerada pelo sistema
    description_client       TEXT,                    -- relato do próprio usuário
    channel                  VARCHAR(60),             -- 'ligacao' | 'whatsapp' | 'sms' | 'email'
    status_transaction       VARCHAR(20) NOT NULL DEFAULT 'pending',
    verification_code        VARCHAR(6),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at              TIMESTAMPTZ,

    CONSTRAINT chk_transactions_status
        CHECK (status_transaction IN ('pending', 'confirmed', 'cancelled', 'expired')),
    CONSTRAINT chk_transactions_confirmed
        CHECK (
            (status_transaction = 'confirmed' AND confirmed_at IS NOT NULL) OR
            (status_transaction <> 'confirmed')
        )
);

CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_status ON transactions (status_transaction) WHERE status_transaction = 'pending';

CREATE TABLE scam_patterns (
    id          SERIAL PRIMARY KEY,
    category    VARCHAR(80)  NOT NULL UNIQUE,
    title       VARCHAR(150) NOT NULL,
    description TEXT         NOT NULL,
    keywords    TEXT[]       NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE alerts (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    transaction_id     UUID REFERENCES transactions(id),  
    pattern_id         INTEGER REFERENCES scam_patterns(id), 

    contact_channel    VARCHAR(20) NOT NULL,   -- 'ligacao' | 'whatsapp' | 'sms' | 'email'
    origin_number      VARCHAR(20) NOT NULL,
    description_alert  TEXT NOT NULL,

    scam_category      VARCHAR(80),            -- preenchido pela IA com base no padrão de golpe detectado
    risk_level         VARCHAR(10),            -- 'low' | 'medium' | 'high'
    risk_score         NUMERIC(5,2),           -- 0.00 a 100.00
    ai_justification   TEXT,
    ai_flags           JSONB NOT NULL DEFAULT '[]', 
    status_ia          VARCHAR(20) NOT NULL DEFAULT 'pending',

    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_alerts_risk_level
        CHECK (risk_level IN ('low', 'medium', 'high') OR risk_level IS NULL),
    CONSTRAINT chk_alerts_status_ia
        CHECK (status_ia IN ('pending', 'evaluated', 'evaluation_failure')),
    CONSTRAINT chk_alerts_score_range
        CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 100))
);

CREATE INDEX idx_alerts_user_id ON alerts (user_id);
CREATE INDEX idx_alerts_category ON alerts (scam_category);
CREATE INDEX idx_alerts_risk_level ON alerts (risk_level);
CREATE INDEX idx_alerts_ai_flags ON alerts USING GIN (ai_flags); -- o comando gin permite busca dentro do array JSON


-- Trigger para manter atualizações dos usuarios diariamente 
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_updated_at();

-- dashboard de alertas e notificações minimo
INSERT INTO scam_patterns (category, title, description, keywords) VALUES
('falso_funcionario_banco',
 'Falso funcionário do banco',
 'Golpista se passa por funcionário do banco por ligação, SMS, WhatsApp ou e-mail e induz a vítima a instalar apps, clicar em links ou cancelar boletos/compras.',
 ARRAY['funcionario do banco', 'cancelar boleto', 'instalar aplicativo', 'codigo de seguranca']);