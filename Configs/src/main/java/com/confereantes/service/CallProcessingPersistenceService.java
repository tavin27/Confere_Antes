package com.confereantes.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.confereantes.model.CallRecording;
import com.confereantes.model.CallTranscription;
import com.confereantes.model.RiskAnalysis;
import com.confereantes.model.RiskSignal;
import com.confereantes.model.enums.CallProcessingStatus;
import com.confereantes.repository.CallRecordingRepository;
import com.confereantes.repository.CallTranscriptionRepository;
import com.confereantes.repository.RiskAnalysisRepository;
import com.confereantes.repository.RiskSignalRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CallProcessingPersistenceService {

    private final CallRecordingRepository callRecordingRepository;
    private final CallTranscriptionRepository callTranscriptionRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskSignalRepository riskSignalRepository;

    public CallProcessingPersistenceService(
            CallRecordingRepository callRecordingRepository,
            CallTranscriptionRepository callTranscriptionRepository,
            RiskAnalysisRepository riskAnalysisRepository,
            RiskSignalRepository riskSignalRepository) {
        this.callRecordingRepository = callRecordingRepository;
        this.callTranscriptionRepository = callTranscriptionRepository;
        this.riskAnalysisRepository = riskAnalysisRepository;
        this.riskSignalRepository = riskSignalRepository;
    }

    @Transactional(readOnly = true)
    public String findAudioUrl(Long callId) {
        return findCall(callId).getAudioUrl();
    }

    @Transactional
    public void updateStatus(Long callId, CallProcessingStatus status) {
        CallRecording call = findCall(callId);
        if (!isTerminal(call.getStatus())) {
            call.setStatus(status);
            call.setFailureReason(null);
        }
    }

    @Transactional
    public void saveTranscription(Long callId, String text, String provider) {
        CallRecording call = findCall(callId);
        CallTranscription transcription = callTranscriptionRepository.findByCallRecordingId(callId)
                .orElseGet(CallTranscription::new);
        transcription.setCallRecording(call);
        transcription.setTranscriptionText(text);
        transcription.setLanguage("pt");
        transcription.setProvider(provider);
        callTranscriptionRepository.save(transcription);
        if (!isTerminal(call.getStatus())) {
            call.setStatus(CallProcessingStatus.TRANSCRIBED);
            call.setFailureReason(null);
        }
    }

    @Transactional
    public void saveAnalysis(Long callId, CallRiskResult result, String provider, String model) {
        saveAnalysis(callId, result, provider, model, null);
    }

    @Transactional
    public void saveAnalysis(Long callId, CallRiskResult result, String provider, String model,
                             MlClassificationResult mlResult) {
        CallRecording call = findCall(callId);
        RiskAnalysis analysis = riskAnalysisRepository.findByCallRecordingId(callId)
                .orElseGet(RiskAnalysis::new);
        analysis.setCallRecording(call);
        analysis.setRiskScore(result.riskScore());
        analysis.setRiskLevel(result.riskLevel());
        analysis.setScam(result.scam());
        analysis.setConfidence(result.confidence());
        analysis.setRecommendation(result.recommendation());
        analysis.setProvider(provider);
        analysis.setModelVersion(model);
        analysis.setRawResponse(result.rawResponse());
        if (mlResult != null) {
            try {
                String mlJson = new tools.jackson.databind.ObjectMapper().writeValueAsString(mlResult);
                analysis.setMlClassification(mlJson);
            } catch (Exception jsonException) {
                // Falha na serialização ML não deve bloquear o fluxo principal
            }
        }
        RiskAnalysis savedAnalysis = riskAnalysisRepository.save(analysis);

        riskSignalRepository.deleteAll(riskSignalRepository.findByRiskAnalysisId(savedAnalysis.getId()));
        List<RiskSignal> signals = result.signals().stream().map(signal -> {
            RiskSignal entity = new RiskSignal();
            entity.setRiskAnalysis(savedAnalysis);
            entity.setSignalType(signal.type());
            entity.setDescription(signal.description());
            entity.setEvidenceText(signal.evidenceText());
            return entity;
        }).toList();
        riskSignalRepository.saveAll(signals);

        if (!isTerminal(call.getStatus())) {
            call.setStatus(CallProcessingStatus.WAITING_USER_DECISION);
            call.setFailureReason(null);
        }
    }

    @Transactional
    public void markFailed(Long callId, String reason) {
        CallRecording call = callRecordingRepository.findById(callId).orElse(null);
        if (call == null || call.getStatus() == CallProcessingStatus.COMPLETED) {
            return;
        }
        call.setStatus(CallProcessingStatus.FAILED);
        call.setFailureReason(limit(reason, 2000));
    }

    private CallRecording findCall(Long callId) {
        return callRecordingRepository.findById(callId)
                .orElseThrow(() -> new EntityNotFoundException("Ligação não encontrada"));
    }

    private boolean isTerminal(CallProcessingStatus status) {
        return status == CallProcessingStatus.COMPLETED || status == CallProcessingStatus.FAILED;
    }

    private String limit(String value, int maximum) {
        if (value == null || value.isBlank()) return "Falha não detalhada no processamento da ligação";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
