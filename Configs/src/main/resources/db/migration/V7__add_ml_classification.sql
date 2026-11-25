-- Adiciona coluna JSONB para armazenar resultado do classificador ML
-- separadamente da análise do Gemini.
-- O campo ml_classification armazena: label, probability, riskLevel,
-- signals, modelVersion, status.

ALTER TABLE risk_analyses
    ADD COLUMN IF NOT EXISTS ml_classification JSONB;

ALTER TABLE alerts
    ADD COLUMN IF NOT EXISTS ml_classification JSONB;

COMMENT ON COLUMN risk_analyses.ml_classification IS
    'Resultado do classificador ML próprio (TF-IDF + LogisticRegression). Estrutura: {label, probability, riskLevel, signals[], modelVersion, status}';

COMMENT ON COLUMN alerts.ml_classification IS
    'Última classificação ML do chat associada ao alerta. Mesma estrutura de risk_analyses.ml_classification.';