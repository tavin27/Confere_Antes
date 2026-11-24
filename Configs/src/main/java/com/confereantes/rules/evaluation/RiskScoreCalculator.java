
package com.confereantes.rules.evaluation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.confereantes.rules.RuleType;

@Component
public class RiskScoreCalculator {

    public int calculate(List<RuleType> detectedRules) {

        int score = 0;

        if (detectedRules.contains(RuleType.ORIENTACAO_DIGITAR_CREDENCIAL)) {
            score += 10;
        }

        if (detectedRules.contains(RuleType.PRESSAO_URGENCIA)) {
            score += 20;
        }

        if (detectedRules.contains(RuleType.AMEACA_BLOQUEIO_CONTA)) {
            score += 30;
        }

        if (detectedRules.contains(RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA)) {
            score += 40;
        }

        if (detectedRules.contains(RuleType.SOLICITACAO_SENHA)) {
            score += 90;
        }

        if (detectedRules.contains(RuleType.SOLICITACAO_CODIGO_AUTENTICACAO)) {
            score += 95;
        }

        boolean hasBlockingThreat =
                detectedRules.contains(RuleType.AMEACA_BLOQUEIO_CONTA);

        boolean hasUrgency =
                detectedRules.contains(RuleType.PRESSAO_URGENCIA);

        boolean hasFinancialTransfer =
                detectedRules.contains(RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA);

        if (hasBlockingThreat && hasUrgency) {
            score += 15;
        }

        if (hasFinancialTransfer && hasUrgency) {
            score += 15;
        }

        if (hasFinancialTransfer && hasBlockingThreat) {
            score += 15;
        }

        return Math.min(score, 100);
    }
}

