
package com.confereantes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final MessageRepository messageRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public MessageService(
            MessageRepository messageRepository,
            AlertRepository alertRepository,
            UserRepository userRepository) {

        this.messageRepository = messageRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
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

        return messageRepository.save(message);
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
