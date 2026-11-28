
package com.confereantes.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.confereantes.model.Alert;
import com.confereantes.model.Message;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.MessageRepository;
import com.confereantes.rules.RuleEngine;
import com.confereantes.rules.RuleResult;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MessageAiReplyWorkerTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private GeminiAiService geminiAiService;

    @Mock
    private MlClassificationService mlClassificationService;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageAiReplyWorker worker;

    @Test
    void shouldExecuteRuleEngineBeforeMlAndGemini() {

        Long alertId = 1L;

        Alert alert = new Alert();

        Message userMessage = new Message();
        userMessage.setAlert(alert);
        userMessage.setContent("Recebi uma mensagem suspeita.");
        userMessage.setSender(MessageSender.USUARIO);
        userMessage.setSource(MessageSource.MANUAL);

        List<Message> history = List.of(userMessage);

        RuleResult ruleResult = new RuleResult(
                false,
                List.of(),
                List.of(),
                0);

        when(messageRepository.findByAlertIdOrderByCreatedAtAsc(alertId))
                .thenReturn(history);

        when(ruleEngine.analyze(history))
                .thenReturn(ruleResult);

        when(geminiAiService.analyzeChatText(anyList()))
                .thenReturn(new GeminiAiService.ChatAnalysisResult(
                        "Tenha cuidado com mensagens suspeitas.",
                        20));

        when(alertRepository.findById(alertId))
                .thenReturn(Optional.of(alert));

        worker.replyToUser(new UserMessageSubmittedEvent(alertId));

        InOrder inOrder = inOrder(
                ruleEngine,
                mlClassificationService,
                geminiAiService);

        inOrder.verify(ruleEngine).analyze(history);

        inOrder.verify(mlClassificationService)
                .classify("Recebi uma mensagem suspeita.");

        inOrder.verify(geminiAiService)
                .analyzeChatText(anyList());
    }
}

