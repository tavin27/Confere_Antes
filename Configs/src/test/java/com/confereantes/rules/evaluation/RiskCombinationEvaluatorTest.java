
package com.confereantes.rules.evaluation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.confereantes.rules.RuleType;

class RiskCombinationEvaluatorTest {

    private RiskCombinationEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RiskCombinationEvaluator();
    }

    @Test
    void shouldNotTriggerAlertWhenNoRulesAreDetected() {
        boolean result = evaluator.shouldTriggerAlert(List.of());

        assertFalse(result);
    }

    @Test
    void shouldTriggerAlertForPasswordRequest() {
        boolean result = evaluator.shouldTriggerAlert(
                List.of(RuleType.SOLICITACAO_SENHA)
        );

        assertTrue(result);
    }

    @Test
    void shouldTriggerAlertForAuthenticationCodeRequest() {
        boolean result = evaluator.shouldTriggerAlert(
                List.of(RuleType.SOLICITACAO_CODIGO_AUTENTICACAO)
        );

        assertTrue(result);
    }

    @Test
    void shouldTriggerAlertForBlockingThreatAndUrgency() {
        boolean result = evaluator.shouldTriggerAlert(List.of(
                RuleType.AMEACA_BLOQUEIO_CONTA,
                RuleType.PRESSAO_URGENCIA
        ));

        assertTrue(result);
    }

    @Test
    void shouldTriggerAlertForFinancialTransferAndUrgency() {
        boolean result = evaluator.shouldTriggerAlert(List.of(
                RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA,
                RuleType.PRESSAO_URGENCIA
        ));

        assertTrue(result);
    }

    @Test
    void shouldTriggerAlertForFinancialTransferAndBlockingThreat() {
        boolean result = evaluator.shouldTriggerAlert(List.of(
                RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA,
                RuleType.AMEACA_BLOQUEIO_CONTA
        ));

        assertTrue(result);
    }

    @Test
    void shouldNotTriggerAlertForUrgencyAlone() {
        boolean result = evaluator.shouldTriggerAlert(
                List.of(RuleType.PRESSAO_URGENCIA)
        );

        assertFalse(result);
    }

    @Test
    void shouldNotTriggerAlertForFinancialTransferAlone() {
        boolean result = evaluator.shouldTriggerAlert(
                List.of(RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA)
        );

        assertFalse(result);
    }

    @Test
    void shouldNotTriggerAlertForBlockingThreatAlone() {
        boolean result = evaluator.shouldTriggerAlert(
                List.of(RuleType.AMEACA_BLOQUEIO_CONTA)
        );

        assertFalse(result);
    }
}

