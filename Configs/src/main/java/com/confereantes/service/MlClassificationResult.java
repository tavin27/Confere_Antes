package com.confereantes.service;

import java.util.List;

/**
 * Resultado da classificação do serviço ML próprio (TF-IDF + LogisticRegression).
 * Este resultado é independente da análise do Gemini e serve como apoio à decisão.
 */
public record MlClassificationResult(
        String label,
        double probability,
        String riskLevel,
        List<String> signals,
        String modelVersion,
        String status) {

    public boolean isConclusive() {
        return "COMPLETED".equals(status);
    }

    public boolean isInconclusive() {
        return "LOW_CONFIDENCE".equals(status) || "INCONCLUSIVO".equals(label);
    }

    public boolean isGolpe() {
        return "GOLPE".equals(label);
    }

    /**
     * Converte o riskLevel do ML (BAIXO/MEDIO/ALTO) para o enum RiskLevel do sistema.
     */
    public com.confereantes.model.enums.RiskLevel toRiskLevelEnum() {
        if (riskLevel == null) return null;
        return switch (riskLevel.toUpperCase()) {
            case "ALTO" -> com.confereantes.model.enums.RiskLevel.HIGH;
            case "MEDIO" -> com.confereantes.model.enums.RiskLevel.MEDIUM;
            default -> com.confereantes.model.enums.RiskLevel.LOW;
        };
    }
}