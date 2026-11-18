package com.confereantes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import com.confereantes.dto.CallRequest;
import com.confereantes.dto.CallResponse;
import com.confereantes.dto.CallResponse.RiskSignalResponse;
import com.confereantes.dto.UserDecisionRequest;
import com.confereantes.model.Alert;
import com.confereantes.model.CallRecording;
import com.confereantes.model.RiskSignal;
import com.confereantes.model.User;
import com.confereantes.model.UserDecisionRecord;
import com.confereantes.model.enums.CallProcessingStatus;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.CallRecordingRepository;
import com.confereantes.repository.CallTranscriptionRepository;
import com.confereantes.repository.RiskAnalysisRepository;
import com.confereantes.repository.RiskSignalRepository;
import com.confereantes.repository.UserDecisionRecordRepository;
import com.confereantes.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CallAnalysisService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final CallRecordingRepository callRecordingRepository;
    private final CallTranscriptionRepository callTranscriptionRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskSignalRepository riskSignalRepository;
    private final UserDecisionRecordRepository userDecisionRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CallAnalysisService(
            AlertRepository alertRepository,
            UserRepository userRepository,
            CallRecordingRepository callRecordingRepository,
            CallTranscriptionRepository callTranscriptionRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            RiskSignalRepository riskSignalRepository,
            UserDecisionRecordRepository userDecisionRecordRepository,
            ApplicationEventPublisher eventPublisher) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.callRecordingRepository = callRecordingRepository;
        this.callTranscriptionRepository = callTranscriptionRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskSignalRepository = riskSignalRepository;
        this.userDecisionRecordRepository = userDecisionRecordRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CallResponse createCall(Long alertId, String userEmail, CallRequest request) {
        User user = findUser(userEmail);
        Alert alert = alertRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Alerta não encontrado"));

        CallRecording call = new CallRecording();
        call.setAlert(alert);
        call.setAudioUrl(request.getAudioUrl());
        call.setPhoneNumber(request.getPhoneNumber());
        call.setStatus(CallProcessingStatus.RECEIVED);

        CallRecording savedCall = callRecordingRepository.save(call);
        eventPublisher.publishEvent(new CallSubmittedEvent(savedCall.getId()));
        return toResponse(savedCall);
    }

    @Transactional(readOnly = true)
    public CallResponse getCall(Long callId, String userEmail) {
        User user = findUser(userEmail);
        CallRecording call = callRecordingRepository.findByIdAndAlert_User_Id(callId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ligação não encontrada"));

        return toResponse(call);
    }

    @Transactional
    public CallResponse registerDecision(Long callId, String userEmail, UserDecisionRequest request) {
        User user = findUser(userEmail);
        CallRecording call = callRecordingRepository.findByIdAndAlert_User_Id(callId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ligação não encontrada"));

        UserDecisionRecord decision = userDecisionRecordRepository.findByCallRecordingId(callId)
                .orElseGet(UserDecisionRecord::new);
        decision.setCallRecording(call);
        decision.setUser(user);
        decision.setDecision(request.getDecision());
        userDecisionRecordRepository.save(decision);

        call.setStatus(CallProcessingStatus.COMPLETED);
        callRecordingRepository.save(call);

        return toResponse(call);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    private CallResponse toResponse(CallRecording call) {
        var transcription = callTranscriptionRepository.findByCallRecordingId(call.getId());
        var analysis = riskAnalysisRepository.findByCallRecordingId(call.getId());
        var decision = userDecisionRecordRepository.findByCallRecordingId(call.getId());

        List<RiskSignalResponse> signals = analysis
                .map(item -> riskSignalRepository.findByRiskAnalysisId(item.getId()).stream()
                        .map(this::toSignalResponse)
                        .toList())
                .orElse(List.of());

        return new CallResponse(
                call.getId(),
                call.getAlert().getId(),
                call.getAudioUrl(),
                call.getPhoneNumber(),
                call.getStatus(),
                call.getCreatedAt(),
                call.getUpdatedAt(),
                transcription.map(item -> item.getTranscriptionText()).orElse(null),
                analysis.map(item -> item.getRiskScore()).orElse(null),
                analysis.map(item -> item.getRiskLevel()).orElse(null),
                analysis.map(item -> item.isScam()).orElse(false),
                analysis.map(item -> item.getConfidence()).orElse(null),
                analysis.map(item -> item.getRecommendation()).orElse(null),
                signals,
                decision.map(item -> item.getDecision()).orElse(null),
                call.getFailureReason());
    }

    private RiskSignalResponse toSignalResponse(RiskSignal signal) {
        return new RiskSignalResponse(
                signal.getSignalType(),
                signal.getDescription(),
                signal.getEvidenceText());
    }
}
