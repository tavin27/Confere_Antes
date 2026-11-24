package com.confereantes.rules;

import java.util.List;

public class RuleResult {

    private final boolean alertTriggered;
    private final List<RuleType> detectedRules;
    private final List<String> reasons;
    private final int riskScore;

    public RuleResult(
            boolean alertTriggered,
            List<RuleType> detectedRules,
            List<String> reasons,
            int riskScore) {

        this.alertTriggered = alertTriggered;
        this.detectedRules = List.copyOf(detectedRules);
        this.reasons = List.copyOf(reasons);
        this.riskScore = riskScore;
    }

    public boolean isAlertTriggered() {
        return alertTriggered;
    }

    public List<RuleType> getDetectedRules() {
        return detectedRules;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public int getRiskScore() {
        return riskScore;
    }
}