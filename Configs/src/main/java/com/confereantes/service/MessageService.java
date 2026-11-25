
package com.confereantes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import com.confereantes.model.Alert;
import com.confereantes.model.Message;
import com.confereantes.model.User;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.MessageRepository;
import com.confereantes.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final GeminiAiService geminiAiService;

    public MessageService(
            MessageRepository messageRepository,
            AlertRepository alertRepository,
            UserRepository userRepository,
            GeminiAiService geminiAiService) {

        this.messageRepository = messageRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.geminiAiService = geminiAiService;
    }

    @Transactional
    public Message createMessage(
            Long alertId,
            String userEmail,
            String content,
            MessageSender sender) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado"));

        Alert alert = alertRepository.findByIdAndUserId(
                alertId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sessão não encontrada"));

        Message message = new Message();

        message.setAlert(alert);
        message.setContent(content);
        message.setSender(sender);
        message.setSource(MessageSource.MANUAL);

        Message saved = messageRepository.save(message);

        if (sender == MessageSender.USUARIO) {
            analyzeAndReplyAsync(alertId, content);
        }

        return saved;
    }

    @Async
    public void analyzeAndReplyAsync(Long alertId, String userMessage) {
        try {
            String reply = geminiAiService.analyzeChatText(userMessage);

            Alert alert = alertRepository.findById(alertId).orElse(null);
            if (alert == null) {
                logger.warn("Alerta {} não encontrado para resposta automática", alertId);
                return;
            }

            Message botMessage = new Message();
            botMessage.setAlert(alert);
            botMessage.setContent(reply);
            botMessage.setSender(MessageSender.CONTATO);
            botMessage.setSource(MessageSource.AUTOMATICO);
            messageRepository.save(botMessage);

            logger.info("Resposta automática salva para alerta {}", alertId);
        } catch (Exception exception) {
            logger.error("Falha ao gerar resposta automática para alerta {}: {}", alertId, exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesByAlert(
            Long alertId,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado"));

        alertRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sessão não encontrada"));

        return messageRepository.findByAlertIdOrderByCreatedAtAsc(alertId);
    }
}
