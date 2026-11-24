package com.confereantes.rules;

public record RuleMatch(
        RuleType ruleType,
        String reason) {
}