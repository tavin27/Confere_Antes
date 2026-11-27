package com.confereantes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.confereantes.dto.CallRequest;
import com.confereantes.dto.CallResponse;
import com.confereantes.dto.UserDecisionRequest;
import com.confereantes.model.enums.CallProcessingStatus;
import com.confereantes.model.enums.UserDecision;
import com.confereantes.service.CallAnalysisService;

@ExtendWith(MockitoExtension.class)
class CallAnalysisControllerTest {

    private static final String USER_EMAIL = "usuario@teste.com";

    @Mock
    private CallAnalysisService callAnalysisService;

    @InjectMocks
    private CallAnalysisController callAnalysisController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(callAnalysisController)
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateCallSuccessfully() throws Exception {
        when(callAnalysisService.createCall(eq(7L), eq(USER_EMAIL), any(CallRequest.class)))
                .thenReturn(callResponse(CallProcessingStatus.RECEIVED, null));

        mockMvc.perform(post("/alerts/7/calls")
                .principal(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"audioUrl":"https://storage.example.test/call.mp3","phoneNumber":"11999999999"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.alertId").value(7))
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(callAnalysisService).createCall(eq(7L), eq(USER_EMAIL), any(CallRequest.class));
    }

    @Test
    void shouldRejectCallWithoutAudioUrl() throws Exception {
        mockMvc.perform(post("/alerts/7/calls")
                .principal(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"phoneNumber":"11999999999"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCallSuccessfully() throws Exception {
        when(callAnalysisService.getCall(21L, USER_EMAIL))
                .thenReturn(callResponse(CallProcessingStatus.COMPLETED, null));

        mockMvc.perform(get("/calls/21").principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(callAnalysisService).getCall(21L, USER_EMAIL);
    }

    @Test
    void shouldRegisterUserDecisionSuccessfully() throws Exception {
        when(callAnalysisService.registerDecision(eq(21L), eq(USER_EMAIL), any(UserDecisionRequest.class)))
                .thenReturn(callResponse(CallProcessingStatus.COMPLETED, UserDecision.BLOCK_NUMBER));

        mockMvc.perform(post("/calls/21/decision")
                .principal(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"decision":"BLOCK_NUMBER"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("BLOCK_NUMBER"));

        verify(callAnalysisService).registerDecision(eq(21L), eq(USER_EMAIL), any(UserDecisionRequest.class));
    }

    @Test
    void shouldRejectDecisionWhenMissing() throws Exception {
        mockMvc.perform(post("/calls/21/decision")
                .principal(authenticatedUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        return UsernamePasswordAuthenticationToken.authenticated(USER_EMAIL, "n/a", List.of());
    }

    private CallResponse callResponse(CallProcessingStatus status, UserDecision decision) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 25, 12, 0);
        return new CallResponse(
                21L,
                7L,
                "https://storage.example.test/call.mp3",
                "11999999999",
                status,
                now,
                now,
                null,
                null,
                null,
                false,
                null,
                null,
                List.of(),
                decision,
                null);
    }
}
