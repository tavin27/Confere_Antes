
package com.confereantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.confereantes.model.Alert;
import com.confereantes.model.Message;
import com.confereantes.model.User;
import com.confereantes.model.enums.ContactChannel;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.MessageRepository;
import com.confereantes.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MessageService messageService;

    @Test
    void shouldCreateMessageSuccessfully() {

        Long alertId = 1L;
        Long userId = 10L;
        String email = "test@example.com";

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Alert alert = new Alert();
        alert.setContactChannel(ContactChannel.WHATSAPP);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(alertRepository.findByIdAndUserId(alertId, userId))
                .thenReturn(Optional.of(alert));

        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Message result = messageService.createMessage(
                alertId,
                email,
                "Olá, sou funcionário do banco.",
                MessageSender.CONTATO);

        assertSame(alert, result.getAlert());

        assertEquals(
                "Olá, sou funcionário do banco.",
                result.getContent());

        assertEquals(MessageSender.CONTATO, result.getSender());

        // A origem deve ser definida pelo próprio backend.
        assertEquals(MessageSource.MANUAL, result.getSource());

        verify(userRepository).findByEmail(email);
        verify(alertRepository).findByIdAndUserId(alertId, userId);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void shouldPublishAiReplyEventForUserMessage() {
        Long alertId = 1L;
        Long userId = 10L;
        String email = "test@example.com";
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        Alert alert = new Alert();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(alertRepository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.of(alert));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.createMessage(alertId, email, "O que eu faco?", MessageSender.USUARIO);

        verify(eventPublisher).publishEvent(new UserMessageSubmittedEvent(alertId));
    }

    @Test
    void shouldThrowExceptionWhenAlertDoesNotBelongToUser() {

        Long alertId = 99L;
        Long userId = 10L;
        String email = "test@example.com";

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(alertRepository.findByIdAndUserId(alertId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> messageService.createMessage(
                        alertId,
                        email,
                        "Mensagem de teste",
                        MessageSender.CONTATO));

        verifyNoInteractions(messageRepository);
    }

    @Test
    void shouldFindMessagesByAlertSuccessfully() {

        Long alertId = 1L;
        Long userId = 10L;
        String email = "test@example.com";

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Alert alert = new Alert();

        List<Message> expectedMessages = List.of(
                createMessage(alert, "Olá!", MessageSender.CONTATO,
                        MessageSource.MANUAL),
                createMessage(alert, "Preciso confirmar seus dados.",
                        MessageSender.CONTATO, MessageSource.AUTOMATICO));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(alertRepository.findByIdAndUserId(alertId, userId))
                .thenReturn(Optional.of(alert));

        when(messageRepository.findByAlertIdOrderByCreatedAtAsc(alertId))
                .thenReturn(expectedMessages);

        List<Message> result = messageService.getMessagesByAlert(alertId, email);

        assertEquals(2, result.size());

        assertEquals("Olá!", result.get(0).getContent());

        assertEquals(
                "Preciso confirmar seus dados.",
                result.get(1).getContent());

        assertEquals(MessageSource.MANUAL, result.get(0).getSource());
        assertEquals(MessageSource.AUTOMATICO, result.get(1).getSource());

        verify(userRepository).findByEmail(email);
        verify(alertRepository).findByIdAndUserId(alertId, userId);

        verify(messageRepository)
                .findByAlertIdOrderByCreatedAtAsc(alertId);
    }

    @Test
    void shouldThrowExceptionWhenAlertDoesNotBelongToUserDuringListing() {

        Long alertId = 99L;
        Long userId = 10L;
        String email = "test@example.com";

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(alertRepository.findByIdAndUserId(alertId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> messageService.getMessagesByAlert(alertId, email));

        verifyNoInteractions(messageRepository);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        String email = "unknown@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> messageService.getMessagesByAlert(1L, email));

        verifyNoInteractions(alertRepository, messageRepository);
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

        return message;
    }
}
