
package com.confereantes.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.confereantes.model.enums.CallProcessingStatus;
import com.confereantes.rules.RuleResult;

@Component
public class CallProcessingWorker {

    private static final Logger logger =
            LoggerFactory.getLogger(CallProcessingWorker.class);

    private final CallProcessingPersistenceService persistenceService;
    private final RemoteAudioFetcher audioFetcher;
    private final GeminiAiService geminiAiService;
    private final MlClassificationService mlClassificationService;
    private final TranscriptParser transcriptParser;
    private final TranscriptRuleEngine transcriptRuleEngine;
    private final String model;

    public CallProcessingWorker(
            CallProcessingPersistenceService persistenceService,
            RemoteAudioFetcher audioFetcher,
            GeminiAiService geminiAiService,
            MlClassificationService mlClassificationService,
            TranscriptParser transcriptParser,
            TranscriptRuleEngine transcriptRuleEngine,
            @Value("${gemini.model:gemini-3.8-flash}") String model) {

        this.persistenceService = persistenceService;
        this.audioFetcher = audioFetcher;
        this.geminiAiService = geminiAiService;
        this.mlClassificationService = mlClassificationService;
        this.transcriptParser = transcriptParser;
        this.transcriptRuleEngine = transcriptRuleEngine;
        this.model = model;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(CallSubmittedEvent event) {

        Long callId = event.callId();

        try {
            geminiAiService.validateConfiguration();

            persistenceService.updateStatus(
                    callId,
                    CallProcessingStatus.TRANSCRIBING
            );

            AudioFile audio = audioFetcher.fetch(
                    persistenceService.findAudioUrl(callId)
            );

            String transcript = geminiAiService.transcribe(audio);

            persistenceService.saveTranscription(
                    callId,
                    transcript,
                    "gemini:" + model
            );

            List<TranscriptSegment> segments =
                    transcriptParser.parse(transcript);

            long uncertainSegments = segments.stream()
                    .filter(TranscriptSegment::hasUncertainSpeaker)
                    .count();

            logger.info(
                    "Transcript parsed for call {}: totalSegments={}, uncertainSegments={}",
                    callId,
                    segments.size(),
                    uncertainSegments
            );

            // Executa e persiste as regras locais antes das análises de IA.
            try {
                RuleResult localResult =
                        transcriptRuleEngine.analyze(segments);

                persistenceService.saveLocalAnalysis(
                        callId,
                        localResult
                );

                logger.info(
                        "Local rule analysis saved for call {}: alertTriggered={}, riskScore={}, detectedRules={}, reasons={}",
                        callId,
                        localResult.isAlertTriggered(),
                        localResult.getRiskScore(),
                        localResult.getDetectedRules(),
                        localResult.getReasons()
                );

            } catch (RuntimeException localException) {
                logger.warn(
                        "Local rule analysis or persistence failed for call {}, continuing with AI analysis: {}",
                        callId,
                        localException.getMessage()
                );
            }

            // A classificação ML continua usando a transcrição completa.
            MlClassificationResult mlResult = null;

            try {
                mlResult = mlClassificationService.classify(transcript);

                if (mlResult != null) {
                    logger.info(
                            "ML classification for call {}: label={} prob={} risk={}",
                            callId,
                            mlResult.label(),
                            mlResult.probability(),
                            mlResult.riskLevel()
                    );
                }

            } catch (Exception mlException) {
                logger.warn(
                        "ML classification failed for call {}, continuing with Gemini only: {}",
                        callId,
                        mlException.getMessage()
                );
            }

            persistenceService.updateStatus(
                    callId,
                    CallProcessingStatus.ANALYZING
            );

            // A Gemini continua recebendo a transcrição completa.
            CallRiskResult result = geminiAiService.analyze(transcript);

            persistenceService.saveAnalysis(
                    callId,
                    result,
                    "gemini",
                    model,
                    mlResult
            );

        } catch (RuntimeException exception) {

            logger.error(
                    "Falha ao processar ligação id={}: {}",
                    callId,
                    exception.getMessage()
            );

            try {
                persistenceService.markFailed(
                        callId,
                        exception.getMessage()
                );

            } catch (RuntimeException persistenceException) {
                logger.error(
                        "Não foi possível registrar a falha da ligação id={}",
                        callId,
                        persistenceException
                );
            }
        }
    }
}
