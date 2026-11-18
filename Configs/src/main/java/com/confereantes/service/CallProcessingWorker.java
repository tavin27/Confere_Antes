package com.confereantes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.confereantes.model.enums.CallProcessingStatus;

@Component
public class CallProcessingWorker {

    private static final Logger logger = LoggerFactory.getLogger(CallProcessingWorker.class);

    private final CallProcessingPersistenceService persistenceService;
    private final RemoteAudioFetcher audioFetcher;
    private final OpenRouterAiService openRouterAiService;
    private final String transcriptionModel;
    private final String analysisModel;

    public CallProcessingWorker(
            CallProcessingPersistenceService persistenceService,
            RemoteAudioFetcher audioFetcher,
            OpenRouterAiService openRouterAiService,
            @Value("${openrouter.transcription-model:openai/whisper-1}") String transcriptionModel,
            @Value("${openrouter.analysis-model:inclusionai/ling-3.2-flash:free}") String analysisModel) {
        this.persistenceService = persistenceService;
        this.audioFetcher = audioFetcher;
        this.openRouterAiService = openRouterAiService;
        this.transcriptionModel = transcriptionModel;
        this.analysisModel = analysisModel;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(CallSubmittedEvent event) {
        Long callId = event.callId();
        try {
            openRouterAiService.validateConfiguration();
            persistenceService.updateStatus(callId, CallProcessingStatus.TRANSCRIBING);
            AudioFile audio = audioFetcher.fetch(persistenceService.findAudioUrl(callId));
            String transcript = openRouterAiService.transcribe(audio);
            persistenceService.saveTranscription(callId, transcript, "openrouter:" + transcriptionModel);

            persistenceService.updateStatus(callId, CallProcessingStatus.ANALYZING);
            CallRiskResult result = openRouterAiService.analyze(transcript);
            persistenceService.saveAnalysis(callId, result, "openrouter", analysisModel);
        } catch (RuntimeException exception) {
            logger.error("Falha ao processar ligação id={}: {}", callId, exception.getMessage());
            try {
                persistenceService.markFailed(callId, exception.getMessage());
            } catch (RuntimeException persistenceException) {
                logger.error("Não foi possível registrar a falha da ligação id={}", callId, persistenceException);
            }
        }
    }
}
