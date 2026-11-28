
package com.confereantes.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.confereantes.model.Alert;
import com.confereantes.model.Message;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.MessageRepository;
import com.confereantes.rules.RuleEngine;
import com.confereantes.rules.RuleResult;

import tools.jackson.databind.ObjectMapper;

@Component
public class MessageAiReplyWorker {

    private static final Logger logger = LoggerFactory.getLogger(MessageAiReplyWorker.class);
    private static final int MAX_CONTEXT_MESSAGES = 12;

    private final MessageRepository messageRepository;
    private final AlertRepository alertRepository;
    private final GeminiAiService geminiAiService;
    private final MlClassificationService mlClassificationService;
    private final RuleEngine ruleEngine;
    private final ObjectMapper objectMapper;

    public MessageAiReplyWorker(
            MessageRepository messageRepository,
            AlertRepository alertRepository,
            GeminiAiService geminiAiService,
            MlClassificationService mlClassificationService,
            RuleEngine ruleEngine,
            ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.alertRepository = alertRepository;
        this.geminiAiService = geminiAiService;
        this.mlClassificationService = mlClassificationService;
        this.ruleEngine = ruleEngine;
        this.objectMapper = objectMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void replyToUser(UserMessageSubmittedEvent event) {
        Long alertId = event.alertId();

        try {
            List<Message> history = messageRepository.findByAlertIdOrderByCreatedAtAsc(alertId);

            // 1. Motor local de regras
            RuleResult ruleResult = ruleEngine.analyze(history);

            logger.info(
                    "Local Rule Engine for alert {}: triggered={} riskScore={} rules={} reasons={}",
                    alertId,
                    ruleResult.isAlertTriggered(),
                    ruleResult.getRiskScore(),
                    ruleResult.getDetectedRules(),
                    ruleResult.getReasons());

            // 2. Montagem do contexto para a Gemini
            List<GeminiAiService.ChatTurn> conversation = history.stream()
                    .skip(Math.max(0, history.size() - MAX_CONTEXT_MESSAGES))
                    .map(message -> new GeminiAiService.ChatTurn(
                            message.getSource() == MessageSource.AUTOMATICO ? "assistant" : "user",
                            message.getContent()))
                    .toList();

            // 3. Classificação ML
            String lastUserText = conversation.stream()
                    .filter(t -> "user".equals(t.role()))
                    .reduce((first, second) -> second)
                    .map(GeminiAiService.ChatTurn::content)
                    .orElse("");

            MlClassificationResult mlResult = null;

            if (!lastUserText.isBlank()) {
                mlResult = mlClassificationService.classify(lastUserText);

                if (mlResult != null) {
                    logger.info(
                            "ML classification for alert {}: label={} prob={} risk={}",
                            alertId,
                            mlResult.label(),
                            mlResult.probability(),
                            mlResult.riskLevel());
                }
            }

            // 4. Chamada da Gemini
            GeminiAiService.ChatAnalysisResult analysis =
                    callWithRetry(conversation, alertId);

            Alert alert = alertRepository.findById(alertId).orElse(null);

            if (alert == null) {
                logger.warn("Alert {} not found; AI reply was discarded", alertId);
                return;
            }

            if (analysis.riskScore() != null) {
                alert.setOverallRiskScore(analysis.riskScore());
            }

            if (mlResult != null) {
                try {
                    String mlJson = objectMapper.writeValueAsString(mlResult);
                    alert.setMlClassification(mlJson);
                } catch (Exception jsonException) {
                    logger.warn(
                            "Failed to serialize ML result for alert {}: {}",
                            alertId,
                            jsonException.getMessage());
                }
            }

            alertRepository.save(alert);

            if (analysis.riskScore() != null) {
                logger.info(
                        "Updated overallRiskScore={} for alert {}",
                        analysis.riskScore(),
                        alertId);
            }

            Message aiMessage = new Message();
            aiMessage.setAlert(alert);
            aiMessage.setContent(analysis.reply());
            aiMessage.setSender(MessageSender.CONTATO);
            aiMessage.setSource(MessageSource.AUTOMATICO);

            messageRepository.save(aiMessage);

            logger.info("Gemini reply saved for alert {}", alertId);

        } catch (Exception exception) {
            logger.error(
                    "Could not generate Gemini reply for alert {}",
                    alertId,
                    exception);
        }
    }

    private GeminiAiService.ChatAnalysisResult callWithRetry(
            List<GeminiAiService.ChatTurn> conversation,
            Long alertId) {

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return geminiAiService.analyzeChatText(conversation);

            } catch (IllegalStateException exception) {

                boolean isTransient = exception.getMessage() != null
                        && (exception.getMessage().contains("503")
                                || exception.getMessage().contains("429"));

                if (!isTransient || attempt == maxAttempts) {
                    throw exception;
                }

                long waitMs = attempt * 3000L;

                logger.warn(
                        "Gemini returned transient error for alert {}, retrying in {}ms (attempt {}/{})",
                        alertId,
                        waitMs,
                        attempt,
                        maxAttempts);

                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
            }
        }

        throw new IllegalStateException("Retry exhausted for alert " + alertId);
    }
}
