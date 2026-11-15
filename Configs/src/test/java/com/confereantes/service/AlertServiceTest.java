
package com.confereantes.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.confereantes.dto.AlertRequest;
import com.confereantes.dto.AlertResponse;
import com.confereantes.model.Alert;
import com.confereantes.model.User;
import com.confereantes.model.enums.ContactChannel;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AlertService alertService;

    @Test
    void shouldCreateAlertSuccessfully() {

        Long userId = 1L;

        User user = new User();

        AlertRequest request = new AlertRequest();
        request.setContactChannel(ContactChannel.WHATSAPP);
        request.setContactValue("11999999999");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> {
                    Alert alert = invocation.getArgument(0);

                    alert.setOverallRiskScore(null);

                    return alert;
                });

        AlertResponse response = alertService.createAlert(request, userId);

        assertNotNull(response);
        assertEquals(ContactChannel.WHATSAPP, response.getContactChannel());
        assertEquals("11999999999", response.getContactValue());
        assertNull(response.getOverallRiskScore());

        verify(userRepository).findById(userId);
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        Long userId = 99L;

        AlertRequest request = new AlertRequest();
        request.setContactChannel(ContactChannel.WHATSAPP);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            alertService.createAlert(request, userId);
        });

        verify(userRepository).findById(userId);
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void shouldCreateAlertWithoutContactValue() {

        Long userId = 1L;

        User user = new User();

        AlertRequest request = new AlertRequest();
        request.setContactChannel(ContactChannel.WHATSAPP);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AlertResponse response = alertService.createAlert(request, userId);

        assertNotNull(response);
        assertEquals(ContactChannel.WHATSAPP, response.getContactChannel());
        assertNull(response.getContactValue());

        verify(alertRepository).save(any(Alert.class));
    }

}
