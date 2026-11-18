package com.confereantes.service;

import java.math.BigDecimal;
import java.util.List;

import com.confereantes.model.enums.RiskLevel;
import com.confereantes.model.enums.RiskRecommendation;

public record CallRiskResult(
        int riskScore,
        RiskLevel riskLevel,
        boolean scam,
        BigDecimal confidence,
        RiskRecommendation recommendation,
        List<Signal> signals,
        String rawResponse) {

    public record Signal(String type, String description, String evidenceText) {
    }
}
