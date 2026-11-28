
package com.confereantes.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.confereantes.model.enums.CallProcessingStatus;
import com.confereantes.rules.RuleResult;

@ExtendWith(MockitoExtension.class)
class CallProcessingWorkerTest {

    @Mock
    private CallProcessingPersistenceService persistenceService;

    @Mock
    private RemoteAudioFetcher audioFetcher;

    @Mock
    private GeminiAiService geminiAiService;

    @Mock
    private MlClassificationService mlClassificationService;

    @Mock
    private TranscriptParser transcriptParser;

    @Mock
    private TranscriptRuleEngine transcriptRuleEngine;

    @Test
    void shouldExecuteLocalRulesBeforeMlAndGeminiAnalysis() {

        Long callId = 1L;

        String audioUrl = "https://example.com/audio.mp3";

        String transcript = """
                PESSOA_1: Bom dia, sou do banco.
                PESSOA_2: Como posso ajudar?
                """;

        AudioFile audio = mock(AudioFile.class);
        RuleResult localResult = mock(RuleResult.class);
        CallRiskResult riskResult = mock(CallRiskResult.class);

        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment("PESSOA_1", "Bom dia, sou do banco."),
                new TranscriptSegment("PESSOA_2", "Como posso ajudar?")
        );

        CallSubmittedEvent event = new CallSubmittedEvent(callId);

        when(persistenceService.findAudioUrl(callId))
                .thenReturn(audioUrl);

        when(audioFetcher.fetch(audioUrl))
                .thenReturn(audio);

        when(geminiAiService.transcribe(audio))
                .thenReturn(transcript);

        when(transcriptParser.parse(transcript))
                .thenReturn(segments);

        when(transcriptRuleEngine.analyze(segments))
                .thenReturn(localResult);

        when(geminiAiService.analyze(transcript))
                .thenReturn(riskResult);

        CallProcessingWorker worker = new CallProcessingWorker(
                persistenceService,
                audioFetcher,
                geminiAiService,
                mlClassificationService,
                transcriptParser,
                transcriptRuleEngine,
                "gemini-test"
        );

        worker.process(event);

        InOrder inOrder = inOrder(
                persistenceService,
                audioFetcher,
                geminiAiService,
                transcriptParser,
                transcriptRuleEngine,
                mlClassificationService
        );

        inOrder.verify(geminiAiService).validateConfiguration();

        inOrder.verify(persistenceService)
                .updateStatus(callId, CallProcessingStatus.TRANSCRIBING);

        inOrder.verify(audioFetcher).fetch(audioUrl);

        inOrder.verify(geminiAiService).transcribe(audio);

        inOrder.verify(persistenceService)
                .saveTranscription(callId, transcript, "gemini:gemini-test");

        inOrder.verify(transcriptParser).parse(transcript);

        inOrder.verify(transcriptRuleEngine).analyze(segments);

        inOrder.verify(mlClassificationService).classify(transcript);

        inOrder.verify(persistenceService)
                .updateStatus(callId, CallProcessingStatus.ANALYZING);

        inOrder.verify(geminiAiService).analyze(transcript);

        verify(persistenceService)
                .saveAnalysis(callId, riskResult, "gemini", "gemini-test", null);
    }

    @Test
    void shouldContinueGeminiAnalysisWhenMlClassificationFails() {

        Long callId = 2L;

        String audioUrl = "https://example.com/audio.mp3";

        String transcript = """
                PESSOA_1: Sua conta será bloqueada.
                PESSOA_2: O que devo fazer?
                """;

        AudioFile audio = mock(AudioFile.class);

        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment("PESSOA_1", "Sua conta será bloqueada."),
                new TranscriptSegment("PESSOA_2", "O que devo fazer?")
        );

        RuleResult localResult = mock(RuleResult.class);
        CallRiskResult riskResult = mock(CallRiskResult.class);

        CallSubmittedEvent event = new CallSubmittedEvent(callId);

        when(persistenceService.findAudioUrl(callId))
                .thenReturn(audioUrl);

        when(audioFetcher.fetch(audioUrl))
                .thenReturn(audio);

        when(geminiAiService.transcribe(audio))
                .thenReturn(transcript);

        when(transcriptParser.parse(transcript))
                .thenReturn(segments);

        when(transcriptRuleEngine.analyze(segments))
                .thenReturn(localResult);

        when(geminiAiService.analyze(transcript))
                .thenReturn(riskResult);

        org.mockito.Mockito.doThrow(
                new RuntimeException("ML indisponível")
        )
                .when(mlClassificationService)
                .classify(transcript);

        CallProcessingWorker worker = new CallProcessingWorker(
                persistenceService,
                audioFetcher,
                geminiAiService,
                mlClassificationService,
                transcriptParser,
                transcriptRuleEngine,
                "gemini-test"
        );

        worker.process(event);

        verify(transcriptRuleEngine).analyze(segments);

        verify(geminiAiService).analyze(transcript);

        verify(persistenceService)
                .saveAnalysis(callId, riskResult, "gemini", "gemini-test", null);
    }
}
