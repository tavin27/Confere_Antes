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
    private final GeminiAiService geminiAiService;
    private final String model;

    public CallProcessingWorker(
            CallProcessingPersistenceService persistenceService,
            RemoteAudioFetcher audioFetcher,
            GeminiAiService geminiAiService,
            @Value("${gemini.model:gemini-3.8-flash}") String model) {
        this.persistenceService = persistenceService;
        this.audioFetcher = audioFetcher;
        this.geminiAiService = geminiAiService;
        this.model = model;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(CallSubmittedEvent event) {
        Long callId = event.callId();
        try {
            geminiAiService.validateConfiguration();
            persistenceService.updateStatus(callId, CallProcessingStatus.TRANSCRIBING);
            AudioFile audio = audioFetcher.fetch(persistenceService.findAudioUrl(callId));
            String transcript = geminiAiService.transcribe(audio);
            persistenceService.saveTranscription(callId, transcript, "gemini:" + model);

            persistenceService.updateStatus(callId, CallProcessingStatus.ANALYZING);
            CallRiskResult result = geminiAiService.analyze(transcript);
            persistenceService.saveAnalysis(callId, result, "gemini", model);
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
