
package com.confereantes.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.confereantes.model.Message;
import com.confereantes.rules.evaluation.RiskCombinationEvaluator;
import com.confereantes.rules.evaluation.RiskScoreCalculator;

@Component
public class RuleEngine {

    private final List<DetectionRule> detectionRules;
    private final RiskCombinationEvaluator combinationEvaluator;
    private final RiskScoreCalculator riskScoreCalculator;

    public RuleEngine(
            List<DetectionRule> detectionRules,
            RiskCombinationEvaluator combinationEvaluator,
            RiskScoreCalculator riskScoreCalculator) {

        this.detectionRules = List.copyOf(detectionRules);
        this.combinationEvaluator = combinationEvaluator;
        this.riskScoreCalculator = riskScoreCalculator;
    }

    public RuleResult analyze(List<Message> messages) {

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
