
package com.confereantes.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.confereantes.dto.CallResponse;
import com.confereantes.model.Alert;
import com.confereantes.model.CallRecording;
import com.confereantes.model.LocalRiskAnalysis;
import com.confereantes.model.User;
import com.confereantes.model.enums.CallProcessingStatus;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.CallRecordingRepository;
import com.confereantes.repository.CallTranscriptionRepository;
import com.confereantes.repository.LocalRiskAnalysisRepository;
import com.confereantes.repository.RiskAnalysisRepository;
import com.confereantes.repository.RiskSignalRepository;
import com.confereantes.repository.UserDecisionRecordRepository;
import com.confereantes.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CallAnalysisServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CallRecordingRepository callRecordingRepository;

    @Mock
    private CallTranscriptionRepository callTranscriptionRepository;

    @Mock
    private LocalRiskAnalysisRepository localRiskAnalysisRepository;

    @Mock
    private RiskAnalysisRepository riskAnalysisRepository;

    @Mock
    private RiskSignalRepository riskSignalRepository;

    @Mock
    private UserDecisionRecordRepository userDecisionRecordRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CallAnalysisService callAnalysisService;

    @Test
    void shouldReturnLocalAnalysisWhenGettingCall() {

        String userEmail = "usuario@teste.com";
        Long callId = 10L;
        Long userId = 7L;

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);

        Alert alert = new Alert();
        alert.setUser(user);

        CallRecording call = new CallRecording();
        ReflectionTestUtils.setField(call, "id", callId);
        call.setAlert(alert);
        call.setAudioUrl("https://teste.com/audio.mp3");
        call.setPhoneNumber("11999999999");
        call.setStatus(CallProcessingStatus.COMPLETED);

        LocalRiskAnalysis localAnalysis = new LocalRiskAnalysis();
        localAnalysis.setRiskScore(65);
        localAnalysis.setAlertTriggered(true);
        localAnalysis.setDetectedRules(
                "[\"AMEACA_BLOQUEIO_CONTA\",\"PRESSAO_URGENCIA\"]"
        );
        localAnalysis.setReasons(
                "[\"Ameaça de bloqueio de conta\",\"Pressão de urgência\"]"
        );

        when(userRepository.findByEmail(userEmail))
                .thenReturn(Optional.of(user));

        when(callRecordingRepository.findByIdAndAlert_User_Id(callId, userId))
                .thenReturn(Optional.of(call));

        when(callTranscriptionRepository.findByCallRecordingId(callId))
                .thenReturn(Optional.empty());

        when(localRiskAnalysisRepository.findByCallRecordingId(callId))
                .thenReturn(Optional.of(localAnalysis));

        when(riskAnalysisRepository.findByCallRecordingId(callId))
                .thenReturn(Optional.empty());

        when(userDecisionRecordRepository.findByCallRecordingId(callId))
                .thenReturn(Optional.empty());

        CallResponse response = callAnalysisService.getCall(callId, userEmail);

        assertNotNull(response);
        assertEquals(65, response.getLocalRiskScore());
        assertTrue(response.getLocalAlertTriggered());

        assertEquals(
                2,
                response.getLocalDetectedRules().size()
        );

        assertTrue(response.getLocalDetectedRules()
                .contains("AMEACA_BLOQUEIO_CONTA"));

        assertTrue(response.getLocalDetectedRules()
                .contains("PRESSAO_URGENCIA"));

        assertEquals(
                2,
                response.getLocalReasons().size()
        );

        assertNull(response.getRiskScore());
        assertNull(response.getRiskLevel());

        verify(localRiskAnalysisRepository)
                .findByCallRecordingId(callId);

        verify(callRecordingRepository)
                .findByIdAndAlert_User_Id(callId, userId);
    }

    @Test
    void shouldThrowExceptionWhenCallDoesNotBelongToUser() {

        String userEmail = "usuario@teste.com";
        Long callId = 10L;

        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);

        when(userRepository.findByEmail(userEmail))
                .thenReturn(Optional.of(user));

        when(callRecordingRepository.findByIdAndAlert_User_Id(callId, 7L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> callAnalysisService.getCall(callId, userEmail)
        );

        verify(localRiskAnalysisRepository, never())
                .findByCallRecordingId(anyLong());
    }
}
