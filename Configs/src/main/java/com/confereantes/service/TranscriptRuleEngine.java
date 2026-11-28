
package com.confereantes.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.confereantes.rules.DetectionRule;
import com.confereantes.rules.RuleMatch;
import com.confereantes.rules.RuleResult;
import com.confereantes.rules.RuleType;
import com.confereantes.rules.evaluation.RiskCombinationEvaluator;
import com.confereantes.rules.evaluation.RiskScoreCalculator;

@Component
public class TranscriptRuleEngine {

    private final List<DetectionRule> detectionRules;
    private final TranscriptMessageAdapter messageAdapter;
    private final RiskCombinationEvaluator combinationEvaluator;
    private final RiskScoreCalculator riskScoreCalculator;

    public TranscriptRuleEngine(
            List<DetectionRule> detectionRules,
            TranscriptMessageAdapter messageAdapter,
            RiskCombinationEvaluator combinationEvaluator,
            RiskScoreCalculator riskScoreCalculator) {

        this.detectionRules = List.copyOf(detectionRules);
        this.messageAdapter = messageAdapter;
        this.combinationEvaluator = combinationEvaluator;
        this.riskScoreCalculator = riskScoreCalculator;
    }

    public RuleResult analyze(List<TranscriptSegment> segments) {

        var messages = messageAdapter.toContactMessages(segments);

        List<RuleMatch> matches = detectionRules.stream()
                .flatMap(rule -> rule.evaluate(messages).stream())
                .toList();

        Map<RuleType, String> reasonsByRule = new LinkedHashMap<>();

        for (RuleMatch match : matches) {
            reasonsByRule.putIfAbsent(
                    match.ruleType(),
                    match.reason()
            );
        }

        List<RuleType> detectedRules =
                new ArrayList<>(reasonsByRule.keySet());

        List<String> reasons =
                new ArrayList<>(reasonsByRule.values());

        boolean alertTriggered =
                combinationEvaluator.shouldTriggerAlert(detectedRules);

        int riskScore =
                riskScoreCalculator.calculate(detectedRules);

        return new RuleResult(
                alertTriggered,
                detectedRules,
                reasons,
                riskScore
        );
    }
}

