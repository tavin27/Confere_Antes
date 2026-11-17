package com.confereantes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.confereantes.model.enums.CallProcessingStatus;
import com.confereantes.model.enums.RiskLevel;
import com.confereantes.model.enums.RiskRecommendation;
import com.confereantes.model.enums.UserDecision;

public class CallResponse {

    private final Long id;
    private final Long alertId;
    private final String audioUrl;
    private final String phoneNumber;
    private final CallProcessingStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String transcriptionText;
    private final Integer riskScore;
    private final RiskLevel riskLevel;
    private final boolean scam;
    private final BigDecimal confidence;
    private final RiskRecommendation recommendation;
    private final List<RiskSignalResponse> signals;
    private final UserDecision decision;

    public CallResponse(
            Long id,
            Long alertId,
            String audioUrl,
            String phoneNumber,
            CallProcessingStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String transcriptionText,
            Integer riskScore,
            RiskLevel riskLevel,
            boolean scam,
            BigDecimal confidence,
            RiskRecommendation recommendation,
            List<RiskSignalResponse> signals,
            UserDecision decision) {
        this.id = id;
        this.alertId = alertId;
        this.audioUrl = audioUrl;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.transcriptionText = transcriptionText;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.scam = scam;
        this.confidence = confidence;
        this.recommendation = recommendation;
        this.signals = signals;
        this.decision = decision;
    }

    public Long getId() { return id; }
    public Long getAlertId() { return alertId; }
    public String getAudioUrl() { return audioUrl; }
    public String getPhoneNumber() { return phoneNumber; }
    public CallProcessingStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getTranscriptionText() { return transcriptionText; }
    public Integer getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public boolean isScam() { return scam; }
    public BigDecimal getConfidence() { return confidence; }
    public RiskRecommendation getRecommendation() { return recommendation; }
    public List<RiskSignalResponse> getSignals() { return signals; }
    public UserDecision getDecision() { return decision; }

    public record RiskSignalResponse(
            String type,
            String description,
            String evidenceText) {
    }
}
