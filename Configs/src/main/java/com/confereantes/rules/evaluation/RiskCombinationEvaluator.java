
package com.confereantes.rules.evaluation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.confereantes.rules.RuleType;

@Component
public class RiskCombinationEvaluator {

    public boolean shouldTriggerAlert(List<RuleType> detectedRules) {

        boolean hasImmediateRule = detectedRules.stream()
                .anyMatch(RuleType::triggersImmediateAlert);

        boolean hasBlockingThreat =
                detectedRules.contains(RuleType.AMEACA_BLOQUEIO_CONTA);

        boolean hasUrgencyPressure =
                detectedRules.contains(RuleType.PRESSAO_URGENCIA);

        boolean hasFinancialTransfer =
                detectedRules.contains(RuleType.SOLICITACAO_TRANSFERENCIA_FINANCEIRA);

        boolean hasBlockingAndUrgency =
                hasBlockingThreat && hasUrgencyPressure;

        boolean hasFinancialTransferAndUrgency =
                hasFinancialTransfer && hasUrgencyPressure;

        boolean hasFinancialTransferAndBlocking =
                hasFinancialTransfer && hasBlockingThreat;

        return hasImmediateRule
                || hasBlockingAndUrgency
                || hasFinancialTransferAndUrgency
                || hasFinancialTransferAndBlocking;
    }
}
