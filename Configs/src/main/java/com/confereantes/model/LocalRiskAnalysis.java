
package com.confereantes.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "local_risk_analyses")
public class LocalRiskAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "call_recording_id",
            nullable = false,
            unique = true
    )
    private CallRecording callRecording;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "alert_triggered", nullable = false)
    private boolean alertTriggered;

    @Column(name = "detected_rules", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String detectedRules;

    @Column(name = "reasons", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String reasons;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CallRecording getCallRecording() {
        return callRecording;
    }

    public void setCallRecording(CallRecording callRecording) {
        this.callRecording = callRecording;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public boolean isAlertTriggered() {
        return alertTriggered;
    }

    public void setAlertTriggered(boolean alertTriggered) {
        this.alertTriggered = alertTriggered;
    }

    public String getDetectedRules() {
        return detectedRules;
    }

    public void setDetectedRules(String detectedRules) {
        this.detectedRules = detectedRules;
    }

    public String getReasons() {
        return reasons;
    }

    public void setReasons(String reasons) {
        this.reasons = reasons;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

