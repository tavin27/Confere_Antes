
package com.confereantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.confereantes.rules.RuleResult;
import com.confereantes.rules.RuleType;
import com.confereantes.rules.detection.BlockingThreatRule;
import com.confereantes.rules.detection.CredentialRule;
import com.confereantes.rules.detection.FinancialTransferRule;
import com.confereantes.rules.detection.UrgencyRule;
import com.confereantes.rules.evaluation.RiskCombinationEvaluator;
import com.confereantes.rules.evaluation.RiskScoreCalculator;

class TranscriptRuleEngineTest {

    private TranscriptRuleEngine transcriptRuleEngine;

    @BeforeEach
    void setUp() {
        TranscriptMessageAdapter adapter = new TranscriptMessageAdapter();

        List<com.confereantes.rules.DetectionRule> rules = List.of(
                new CredentialRule(),
                new BlockingThreatRule(),
                new FinancialTransferRule(),
                new UrgencyRule()
        );

        transcriptRuleEngine = new TranscriptRuleEngine(
                rules,
                adapter,
                new RiskCombinationEvaluator(),
                new RiskScoreCalculator()
        );
    }

    @Test
    void shouldDetectPasswordRequestInTranscript() {

        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(
                        "PESSOA_1",
                        "Informe sua senha para confirmar sua identidade."
                )
        );

        RuleResult result = transcriptRuleEngine.analyze(segments);

        assertTrue(result.isAlertTriggered());
        assertTrue(result.getDetectedRules().contains(
                RuleType.SOLICITACAO_SENHA
        ));
        assertEquals(90, result.getRiskScore());
    }

    @Test
    void shouldDetectBlockingThreatCombinedWithUrgency() {

        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(
                        "PESSOA_1",
                        "Sua conta será bloqueada imediatamente."
                )
        );

        RuleResult result = transcriptRuleEngine.analyze(segments);

        assertTrue(result.isAlertTriggered());
        assertTrue(result.getDetectedRules().contains(
                RuleType.AMEACA_BLOQUEIO_CONTA
        ));
        assertTrue(result.getDetectedRules().contains(
                RuleType.PRESSAO_URGENCIA
        ));
        assertEquals(65, result.getRiskScore());
    }

    @Test
    void shouldDetectPasswordRequestWhenSpeakerIsUncertain() {

        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(
                        "PARTICIPANTE_INCERTO",
                        "Informe sua senha imediatamente."
                )
        );

        RuleResult result = transcriptRuleEngine.analyze(segments);

        assertTrue(result.isAlertTriggered());
        assertTrue(result.getDetectedRules().contains(
                RuleType.SOLICITACAO_SENHA
        ));
        assertTrue(result.getDetectedRules().contains(
                RuleType.PRESSAO_URGENCIA
        ));
        assertEquals(100, result.getRiskScore());
    }
}
