
package com.confereantes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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

import com.confereantes.dto.AlertResponse;
import com.confereantes.model.enums.ContactChannel;
import com.confereantes.service.AlertService;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private AlertController alertController;

    private MockMvc mockMvc;

    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(alertController)
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateAlertSuccessfully() throws Exception {

        AlertResponse response = new AlertResponse(
                1L,
                ContactChannel.WHATSAPP,
                "11999999999",
                null,
                LocalDateTime.of(2026, 11, 15, 15, 0));

        when(alertService.createAlert(any(), eq("usuario@teste.com")))
                .thenReturn(response);

        mockMvc.perform(post("/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(new UsernamePasswordAuthenticationToken(
                        "usuario@teste.com",
                        "senha"))
                .content("""
                        {
                            "contactChannel": "WHATSAPP",
                            "contactValue": "11999999999"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.contactChannel").value("WHATSAPP"))
                .andExpect(jsonPath("$.contactValue").value("11999999999"))
                .andExpect(jsonPath("$.overallRiskScore").value((Object) null));

        verify(alertService).createAlert(any(), eq("usuario@teste.com"));
    }

    @Test
    void shouldReturnBadRequestWhenContactChannelIsMissing() throws Exception {

        mockMvc.perform(post("/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(new UsernamePasswordAuthenticationToken(
                        "usuario@teste.com",
                        "senha"))
                .content("""
                        {
                            "contactValue": "11999999999"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }
}
