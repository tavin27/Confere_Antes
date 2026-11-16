
package com.confereantes.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.confereantes.config.JwtAuthenticationEntryPoint;
import com.confereantes.config.JwtConfig;
import com.confereantes.config.SecurityConfig;
import com.confereantes.model.Message;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;
import com.confereantes.service.MessageService;

@WebMvcTest(MessageController.class)
@Import({ SecurityConfig.class, JwtConfig.class })
@TestPropertySource(properties = {
        "jwt.secret=confere-antes-test-secret-key-123456789",
        "jwt.expiration-seconds=3600"
})
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Test
    void shouldCreateMessageSuccessfully() throws Exception {

        String email = "test@example.com";

        Message message = new Message();
        message.setContent("Olá, sou funcionário do banco.");
        message.setSender(MessageSender.CONTATO);
        message.setSource(MessageSource.MANUAL);

        when(messageService.createMessage(
                eq(1L),
                eq(email),
                eq("Olá, sou funcionário do banco."),
                eq(MessageSender.CONTATO)))
                .thenAnswer(invocation -> message);

        String requestBody = """
                {
                    "content": "Olá, sou funcionário do banco.",
                    "sender": "CONTATO"
                }
                """;

        mockMvc.perform(post("/alerts/1/messages")
                .with(jwt().jwt(jwt -> jwt
                        .tokenValue("test-token")
                        .subject(email)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content")
                        .value("Olá, sou funcionário do banco."))
                .andExpect(jsonPath("$.sender").value("CONTATO"))
                .andExpect(jsonPath("$.source").value("MANUAL"));

        verify(messageService).createMessage(
                eq(1L),
                eq(email),
                eq("Olá, sou funcionário do banco."),
                eq(MessageSender.CONTATO));
    }

    @Test
    void shouldIgnoreSourceProvidedByClient() throws Exception {

        String email = "test@example.com";

        Message message = new Message();
        message.setContent("Olá, sou funcionário do banco.");
        message.setSender(MessageSender.CONTATO);
        message.setSource(MessageSource.MANUAL);

        when(messageService.createMessage(
                eq(1L),
                eq(email),
                eq("Olá, sou funcionário do banco."),
                eq(MessageSender.CONTATO)))
                .thenReturn(message);

        String requestBody = """
                {
                    "content": "Olá, sou funcionário do banco.",
                    "sender": "CONTATO",
                    "source": "AUTOMATICO"
                }
                """;

        mockMvc.perform(post("/alerts/1/messages")
                .with(jwt().jwt(jwt -> jwt
                        .tokenValue("test-token")
                        .subject(email)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL"));

        verify(messageService).createMessage(
                eq(1L),
                eq(email),
                eq("Olá, sou funcionário do banco."),
                eq(MessageSender.CONTATO));
    }

    @Test
    void shouldRejectMessageWithBlankContent() throws Exception {

        String requestBody = """
                {
                    "content": "   ",
                    "sender": "CONTATO"
                }
                """;

        mockMvc.perform(post("/alerts/1/messages")
                .with(jwt().jwt(jwt -> jwt
                        .tokenValue("test-token")
                        .subject("test@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindMessagesSuccessfully() throws Exception {

        String email = "test@example.com";

        Message message = new Message();
        message.setContent("Olá, sou funcionário do banco.");
        message.setSender(MessageSender.CONTATO);
        message.setSource(MessageSource.MANUAL);

        when(messageService.getMessagesByAlert(1L, email))
                .thenReturn(List.of(message));

        mockMvc.perform(get("/alerts/1/messages")
                .with(jwt().jwt(jwt -> jwt
                        .tokenValue("test-token")
                        .subject(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content")
                        .value("Olá, sou funcionário do banco."))
                .andExpect(jsonPath("$[0].sender").value("CONTATO"))
                .andExpect(jsonPath("$[0].source").value("MANUAL"));

        verify(messageService).getMessagesByAlert(1L, email);
    }

    @Test
    void shouldReturnEmptyListWhenAlertHasNoMessages() throws Exception {

        String email = "test@example.com";

        when(messageService.getMessagesByAlert(1L, email))
                .thenReturn(List.of());

        mockMvc.perform(get("/alerts/1/messages")
                .with(jwt().jwt(jwt -> jwt
                        .tokenValue("test-token")
                        .subject(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(messageService).getMessagesByAlert(1L, email);
    }
}
