
package com.confereantes.dto;

import java.time.LocalDateTime;

import com.confereantes.model.enums.ContactChannel;

public class AlertResponse {

    private Long id;
    private ContactChannel contactChannel;
    private String contactValue;
    private Integer overallRiskScore;
    private LocalDateTime createdAt;

    public AlertResponse() {
    }

    public AlertResponse(Long id, ContactChannel contactChannel,
            String contactValue, Integer overallRiskScore,
            LocalDateTime createdAt) {
        this.id = id;
        this.contactChannel = contactChannel;
        this.contactValue = contactValue;
        this.overallRiskScore = overallRiskScore;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public ContactChannel getContactChannel() {
        return contactChannel;
    }

    public String getContactValue() {
        return contactValue;
    }

    public Integer getOverallRiskScore() {
        return overallRiskScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
