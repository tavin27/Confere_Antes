
package com.confereantes.rules.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.confereantes.rules.RuleType;

class RiskScoreCalculatorTest {

    private RiskScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskScoreCalculator();
    }

    @Test
    void shouldReturnZeroWhenNoRulesAreDetected() {
        int score = calculator.calculate(List.of());

        assertEquals(0, score);
    }

    @Test
    void shouldCalculateSingleRuleScore() {
        int score = calculator.calculate(
                List.of(RuleType.PRESSAO_URGENCIA)
        );

        assertEquals(20, score);
    }

    @Test
    void shouldCalculateCombinedRulesScore() {
        int score = calculator.calculate(List.of(
                RuleType.AMEACA_BLOQUEIO_CONTA,
                RuleType.PRESSAO_URGENCIA
        ));

        assertEquals(65, score);
    }

    @Test
    void shouldAddFinancialTransferAndUrgencyBonus() {
        int score = calculator.calculate(List.of(
                RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA,
                RuleType.PRESSAO_URGENCIA
        ));

        assertEquals(75, score);
    }

    @Test
    void shouldAddFinancialTransferAndBlockingBonus() {
        int score = calculator.calculate(List.of(
                RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA,
                RuleType.AMEACA_BLOQUEIO_CONTA
        ));

        assertEquals(85, score);
    }

    @Test
    void shouldCalculatePasswordRequestScore() {
        int score = calculator.calculate(
                List.of(RuleType.SOLICITACAO_SENHA)
        );

        assertEquals(90, score);
    }

    @Test
    void shouldCalculateAuthenticationCodeRequestScore() {
        int score = calculator.calculate(
                List.of(RuleType.SOLICITACAO_CODIGO_AUTENTICACAO)
        );

        assertEquals(95, score);
    }

    @Test
    void shouldNeverExceedOneHundred() {
        int score = calculator.calculate(List.of(
                RuleType.SOLICITACAO_SENHA,
                RuleType.SOLICITACAO_CODIGO_AUTENTICACAO,
                RuleType.ORIENTACAO_DIGITAR_CREDENCIAL,
                RuleType.PRESSAO_URGENCIA,
                RuleType.AMEACA_BLOQUEIO_CONTA,
                RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA
        ));

        assertEquals(100, score);
    }
}

