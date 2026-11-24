
package com.confereantes.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.confereantes.model.Message;
import com.confereantes.rules.evaluation.RiskCombinationEvaluator;
import com.confereantes.rules.evaluation.RiskScoreCalculator;

class RuleEngineTest {

    private RiskCombinationEvaluator combinationEvaluator;
    private RiskScoreCalculator riskScoreCalculator;

    @BeforeEach
    void setUp() {
        combinationEvaluator = new RiskCombinationEvaluator();
        riskScoreCalculator = new RiskScoreCalculator();
    }

    @Test
    void shouldReturnZeroScoreWhenNoRulesAreDetected() {
        DetectionRule rule = mock(DetectionRule.class);

        when(rule.evaluate(List.of())).thenReturn(List.of());

        RuleEngine engine = new RuleEngine(
                List.of(rule),
                combinationEvaluator,
                riskScoreCalculator
        );

        RuleResult result = engine.analyze(List.of());

        assertFalse(result.isAlertTriggered());
        assertTrue(result.getDetectedRules().isEmpty());
        assertTrue(result.getReasons().isEmpty());
        assertEquals(0, result.getRiskScore());
    }

    @Test
    void shouldDetectPasswordRequestAndTriggerAlert() {
        DetectionRule rule = mock(DetectionRule.class);

        when(rule.evaluate(List.of())).thenReturn(List.of(
                new RuleMatch(
                        RuleType.SOLICITACAO_SENHA,
                        "Solicitação de senha identificada."
                )
        ));

        RuleEngine engine = new RuleEngine(
                List.of(rule),
                combinationEvaluator,
                riskScoreCalculator
        );

        RuleResult result = engine.analyze(List.of());

        assertTrue(result.isAlertTriggered());
        assertTrue(result.getDetectedRules().contains(
                RuleType.SOLICITACAO_SENHA
        ));
        assertEquals(90, result.getRiskScore());
    }

    @Test
    void shouldCalculateCombinedRiskScore() {
        DetectionRule rule = mock(DetectionRule.class);

        when(rule.evaluate(List.of())).thenReturn(List.of(
                new RuleMatch(
                        RuleType.AMEACA_BLOQUEIO_CONTA,
                        "Ameaça de bloqueio identificada."
                ),
                new RuleMatch(
                        RuleType.PRESSAO_URGENCIA,
                        "Pressão de urgência identificada."
                )
        ));

        RuleEngine engine = new RuleEngine(
                List.of(rule),
                combinationEvaluator,
                riskScoreCalculator
        );

        RuleResult result = engine.analyze(List.of());

        assertTrue(result.isAlertTriggered());
        assertEquals(65, result.getRiskScore());
        assertEquals(2, result.getDetectedRules().size());
        assertEquals(2, result.getReasons().size());
    }

    @Test
    void shouldNotDuplicateDetectedRules() {
        DetectionRule rule = mock(DetectionRule.class);

        when(rule.evaluate(List.of())).thenReturn(List.of(
                new RuleMatch(
                        RuleType.PRESSAO_URGENCIA,
                        "Primeiro motivo."
                ),
                new RuleMatch(
                        RuleType.PRESSAO_URGENCIA,
                        "Segundo motivo."
                )
        ));

        RuleEngine engine = new RuleEngine(
                List.of(rule),
                combinationEvaluator,
                riskScoreCalculator
        );

        RuleResult result = engine.analyze(List.of());

        assertEquals(1, result.getDetectedRules().size());
        assertEquals(1, result.getReasons().size());
        assertEquals("Primeiro motivo.", result.getReasons().get(0));
    }

    @Test
    void shouldExecuteMultipleDetectionRules() {
        DetectionRule firstRule = mock(DetectionRule.class);
        DetectionRule secondRule = mock(DetectionRule.class);

        when(firstRule.evaluate(List.of())).thenReturn(List.of(
                new RuleMatch(
                        RuleType.AMEACA_BLOQUEIO_CONTA,
                        "Ameaça de bloqueio identificada."
                )
        ));

        when(secondRule.evaluate(List.of())).thenReturn(List.of(
                new RuleMatch(
                        RuleType.PRESSAO_URGENCIA,
                        "Pressão de urgência identificada."
                )
        ));

        RuleEngine engine = new RuleEngine(
                List.of(firstRule, secondRule),
                combinationEvaluator,
                riskScoreCalculator
        );

        RuleResult result = engine.analyze(List.of());

        assertEquals(2, result.getDetectedRules().size());
        assertEquals(65, result.getRiskScore());
        assertTrue(result.isAlertTriggered());
    }
}
