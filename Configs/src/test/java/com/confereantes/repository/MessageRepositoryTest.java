
package com.confereantes.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.confereantes.model.Alert;
import com.confereantes.model.Message;
import com.confereantes.model.User;
import com.confereantes.model.enums.ContactChannel;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;

@DataJpaTest
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Alert alert;

    @BeforeEach
    void setUp() {

        User user = new User();

        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.saveAndFlush(user);

        alert = new Alert();

        alert.setUser(savedUser);
        alert.setContactChannel(ContactChannel.WHATSAPP);

        alert = alertRepository.saveAndFlush(alert);
    }

    @Test
    void shouldFindMessagesByAlertOrderedByCreationDate() {

        Message newerMessage = createMessage(
                alert,
                "Informe o código recebido.",
                MessageSender.CONTATO,
                MessageSource.MANUAL);

        Message olderMessage = createMessage(
                alert,
                "Olá, sou funcionário do banco.",
                MessageSender.CONTATO,
                MessageSource.MANUAL);

        Alert anotherAlert = new Alert();
        anotherAlert.setUser(alert.getUser());
        anotherAlert.setContactChannel(ContactChannel.SMS);

        Alert savedAnotherAlert = alertRepository.saveAndFlush(anotherAlert);

        createMessage(
                savedAnotherAlert,
                "Mensagem de outra sessão.",
                MessageSender.CONTATO,
                MessageSource.MANUAL);

        jdbcTemplate.update(
                "UPDATE messages SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.of(2026, 11, 16, 10, 2)),
                newerMessage.getId());

        jdbcTemplate.update(
                "UPDATE messages SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.of(2026, 11, 16, 10, 1)),
                olderMessage.getId());

        List<Message> messages = messageRepository.findByAlertIdOrderByCreatedAtAsc(alert.getId());

        assertEquals(2, messages.size());

        assertEquals(
                "Olá, sou funcionário do banco.",
                messages.get(0).getContent());

        assertEquals(
                "Informe o código recebido.",
                messages.get(1).getContent());
    }

    private Message createMessage(
            Alert alert,
            String content,
            MessageSender sender,
            MessageSource source) {

        Message message = new Message();

        message.setAlert(alert);
        message.setContent(content);
        message.setSender(sender);
        message.setSource(source);

        return messageRepository.saveAndFlush(message);
    }
}
